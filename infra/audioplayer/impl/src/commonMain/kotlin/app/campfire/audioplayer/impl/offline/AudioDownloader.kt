// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.Media
import app.campfire.core.model.PodcastEpisode
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import app.campfire.offline.Downloader
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Audiobooks and podcast episodes, kept by this platform's [OfflineDownloadManager]. The manager
 * works from whole items, so each content's item is looked up first.
 */
@ContributesIntoSet(UserScope::class, binding = binding<Downloader>())
@Inject
class AudioDownloader(
  private val offlineDownloadManager: OfflineDownloadManager,
  private val libraryItemRepository: LibraryItemRepository,
  @ForScope(UserScope::class) private val coroutineScopeHolder: CoroutineScopeHolder,
) : Downloader {

  /** Changes run one at a time, in the order asked, so a delete never overtakes its download. */
  private val changes = Mutex()

  override fun handles(content: DownloadContent): Boolean =
    content is DownloadContent.Audiobook || content is DownloadContent.Episode

  override fun observeAll(): Flow<List<Download>> = offlineDownloadManager.observeAll()
    .map { downloads -> downloads.mapNotNull { it.asDownload() } }

  override fun observe(contents: Set<DownloadContent>): Flow<Map<DownloadContent, Download>> {
    val byItem = contents.groupBy { it.itemId }.map { (itemId, contents) -> observeItem(itemId, contents) }
    if (byItem.isEmpty()) return flowOf(emptyMap())
    return combine(byItem) { downloads ->
      downloads.fold(emptyMap<DownloadContent, Download>()) { all, some -> all + some }
    }.distinctUntilChanged()
  }

  private fun observeItem(
    itemId: LibraryItemId,
    contents: List<DownloadContent>,
  ): Flow<Map<DownloadContent, Download>> = flow {
    val item = itemOrNull(itemId) ?: return@flow emit(emptyMap())
    val episodes = item.episodes(contents.filterIsInstance<DownloadContent.Episode>())
    val downloads = buildList<Flow<List<OfflineDownload>>> {
      if (DownloadContent.Audiobook(itemId) in contents) {
        add(offlineDownloadManager.observeForItem(item).map { listOf(it) })
      }
      when (episodes.size) {
        0 -> Unit
        1 -> add(offlineDownloadManager.observeForEpisode(item, episodes.single()).map { listOf(it) })
        else -> add(offlineDownloadManager.observeForEpisodes(item, episodes).map { it.values.toList() })
      }
    }
    if (downloads.isEmpty()) return@flow emit(emptyMap())
    emitAll(
      combine(downloads) { all ->
        all.toList().flatten().mapNotNull { it.asDownload() }.associateBy { it.content }
      },
    )
  }

  override fun download(contents: List<DownloadContent>) = inOrder {
    contents.groupBy { it.itemId }.forEach { (itemId, contents) ->
      val item = itemOrNull(itemId) ?: return@forEach
      if (DownloadContent.Audiobook(itemId) in contents) offlineDownloadManager.download(item)
      item.episodes(contents.filterIsInstance<DownloadContent.Episode>()).forEach { episode ->
        offlineDownloadManager.downloadEpisode(item, episode)
      }
    }
  }

  override fun stop(content: DownloadContent) = inOrder {
    val item = itemOrNull(content.itemId) ?: return@inOrder
    when (content) {
      is DownloadContent.Audiobook -> offlineDownloadManager.stop(item)
      is DownloadContent.Episode -> item.episodes(listOf(content)).forEach {
        offlineDownloadManager.stopEpisode(item, it)
      }
      is DownloadContent.Ebook -> Unit
    }
  }

  override fun delete(content: DownloadContent) = inOrder {
    val item = itemOrNull(content.itemId) ?: return@inOrder
    when (content) {
      is DownloadContent.Audiobook -> offlineDownloadManager.delete(item)
      is DownloadContent.Episode -> item.episodes(listOf(content)).forEach {
        offlineDownloadManager.deleteEpisode(item, it)
      }
      is DownloadContent.Ebook -> Unit
    }
  }

  override suspend fun deleteAll(itemId: LibraryItemId) {
    changes.withLock { offlineDownloadManager.deleteAllForItemId(itemId) }
  }

  /**
   * Runs [change] after every change asked before it. Starting undispatched takes the lock's
   * queue in call order, whatever thread the scope runs on.
   */
  private fun inOrder(change: suspend () -> Unit) {
    coroutineScopeHolder.get().launch(start = CoroutineStart.UNDISPATCHED) {
      changes.withLock { change() }
    }
  }

  private suspend fun itemOrNull(itemId: LibraryItemId): LibraryItem? = try {
    libraryItemRepository.getLibraryItem(itemId)
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    bark(LogPriority.WARN, throwable = e) { "Couldn't find item $itemId for its audio downloads" }
    null
  }

  private fun LibraryItem.episodes(contents: List<DownloadContent.Episode>): List<PodcastEpisode> {
    val episodeIds = contents.mapTo(HashSet()) { it.episodeId }
    if (episodeIds.isEmpty()) return emptyList()
    return (media as? Media.Podcast)?.episodes.orEmpty().filter { it.id in episodeIds }
  }
}

/** The audio download as every kind of download shows, or null if there isn't one. */
internal fun OfflineDownload.asDownload(): Download? {
  val state = when (state) {
    OfflineDownload.State.None -> return null
    OfflineDownload.State.Queued -> Download.State.Queued
    OfflineDownload.State.Downloading -> Download.State.Downloading
    OfflineDownload.State.Stopped -> Download.State.Stopped
    OfflineDownload.State.Completed -> Download.State.Downloaded
    OfflineDownload.State.Failed -> Download.State.Failed
  }
  return Download(
    content = episodeId?.let { DownloadContent.Episode(libraryItemId, it) }
      ?: DownloadContent.Audiobook(libraryItemId),
    state = state,
    bytes = progress.bytes,
    totalBytes = contentLength,
    progress = progress.percent.takeUnless { progress.indeterminate },
    updatedAtMs = updateTimeMs,
  )
}

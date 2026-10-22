// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader

import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItemId
import app.campfire.core.time.FatherTime
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import app.campfire.offline.Downloader
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path

/**
 * Keeps ebooks on this device to read offline, in [EbookFiles]' download directory. A book already
 * opened is moved over from the cache rather than fetched again.
 *
 * A finished download's file is marked modified when it finished, so the time survives a restart.
 */
@SingleIn(UserScope::class)
@ContributesIntoSet(UserScope::class, binding = binding<Downloader>())
@Inject
class EbookDownloader(
  private val libraryItemRepository: LibraryItemRepository,
  private val files: EbookFiles,
  private val dispatcherProvider: DispatcherProvider,
  private val fatherTime: FatherTime,
  @ForScope(UserScope::class) private val coroutineScopeHolder: CoroutineScopeHolder,
) : Downloader {

  /** Downloads started or finished this session; the rest are read from the disk. */
  private val downloads = MutableStateFlow<Map<LibraryItemId, Download>>(emptyMap())

  override fun handles(content: DownloadContent): Boolean = content is DownloadContent.Ebook

  /** Ebooks downloaded or on their way. A failed download only shows on its own item. */
  override fun observeAll(): Flow<List<Download>> = downloads
    .onStart {
      val onDisk = withContext(dispatcherProvider.io) { files.downloadedFiles() }
      downloads.update { current ->
        current + onDisk.filterKeys { it !in current }.mapValues { (itemId, file) -> downloaded(itemId, file) }
      }
    }
    .map { downloads -> downloads.values.filter { it.state != Download.State.Failed } }
    .distinctUntilChanged()

  override fun observe(contents: Set<DownloadContent>): Flow<Map<DownloadContent, Download>> {
    val itemIds = contents.filterIsInstance<DownloadContent.Ebook>().mapTo(LinkedHashSet()) { it.itemId }
    return downloads
      .onStart {
        val unknown = itemIds - downloads.value.keys
        if (unknown.isEmpty()) return@onStart
        val onDisk = withContext(dispatcherProvider.io) {
          unknown.mapNotNull { itemId -> files.downloadedFile(itemId)?.let { itemId to downloaded(itemId, it) } }
        }
        downloads.update { current -> current + onDisk.filter { (itemId, _) -> itemId !in current } }
      }
      .map { downloads -> itemIds.mapNotNull { downloads[it] }.associateBy { it.content } }
      .distinctUntilChanged()
  }

  override fun download(contents: List<DownloadContent>) {
    contents.filterIsInstance<DownloadContent.Ebook>().forEach { download(it.itemId) }
  }

  private fun download(itemId: LibraryItemId) {
    if (downloads.value[itemId]?.state == Download.State.Downloading) return
    setDownload(itemId, Download.State.Downloading)
    coroutineScopeHolder.get().launch {
      try {
        val item = libraryItemRepository.getLibraryItem(itemId)
        val format = requireNotNull(item.media.ebookFormat) { "Item $itemId has no ebook" }
        val target = files.downloaded(itemId, format)
        val cached = files.cached(itemId, format)
        withContext(dispatcherProvider.io) {
          if (cached.hasContent()) {
            moveFile(cached, target)
          } else {
            // About every percent, rather than every chunk
            var reported = 0L
            files.fetch(itemId, target) { bytes, total ->
              val step = if (total > 0) total / 100 else PROGRESS_STEP_UNKNOWN_SIZE
              if (bytes - reported >= step) {
                reported = bytes
                setDownload(itemId, Download.State.Downloading, bytes, total)
              }
            }
          }
          // A moved file still has the time it was opened
          target.setModifiedAtMs(fatherTime.nowInEpochMillis())
        }
        downloads.update { it + (itemId to downloaded(itemId, target)) }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        bark(LogPriority.WARN, throwable = e) { "Couldn't download the ebook for $itemId" }
        setDownload(itemId, Download.State.Failed)
      }
    }
  }

  /** An ebook's single file can't be stopped part way. */
  override fun stop(content: DownloadContent) = Unit

  override fun delete(content: DownloadContent) {
    downloads.update { it - content.itemId }
    coroutineScopeHolder.get().launch(dispatcherProvider.io) {
      files.deleteDownloaded(content.itemId)
    }
  }

  /** Every copy of the item's ebook, the cached one included, for an item gone from the server. */
  override suspend fun deleteAll(itemId: LibraryItemId) {
    downloads.update { it - itemId }
    withContext(dispatcherProvider.io) {
      files.deleteDownloaded(itemId)
      files.deleteCached(itemId)
    }
  }

  private fun setDownload(itemId: LibraryItemId, state: Download.State, bytes: Long = 0L, total: Long = -1L) {
    val download = Download(
      content = DownloadContent.Ebook(itemId),
      state = state,
      bytes = bytes,
      totalBytes = total,
      progress = if (total > 0) bytes.toFloat() / total else null,
      updatedAtMs = fatherTime.nowInEpochMillis(),
    )
    downloads.update { it + (itemId to download) }
  }

  private fun downloaded(itemId: LibraryItemId, file: Path): Download {
    val size = file.size()
    return Download(
      content = DownloadContent.Ebook(itemId),
      state = Download.State.Downloaded,
      bytes = size,
      totalBytes = size,
      progress = 1f,
      updatedAtMs = file.modifiedAtMs(),
    )
  }
}

private const val PROGRESS_STEP_UNKNOWN_SIZE = 256 * 1024L

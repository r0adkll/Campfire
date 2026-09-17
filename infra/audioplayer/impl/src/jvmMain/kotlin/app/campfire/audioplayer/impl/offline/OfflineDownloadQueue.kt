// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.audioplayer.offline.OfflineDownloadKey
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.core.logging.Cork
import app.campfire.core.model.AudioTrack
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.core.model.User
import app.campfire.core.model.loggableId
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * The desktop offline download engine: fetches every audio file of a book (or a single podcast
 * episode) into [store], a few downloads at a time, and exposes their progress.
 *
 * - Requests are persisted as a [DownloadManifest] before any byte is fetched, so a download
 *   interrupted by a crash or quit resumes from its `.part` files on [resumeDownloads].
 * - Mutations (enqueue, delete, resume) run one at a time, in order, so a delete always cancels
 *   and waits out the download it removes before its files go.
 * - Transient failures retry after [retryDelays], resuming where they left off; a download that
 *   exhausts them (or fails permanently) reports [OfflineDownload.State.Failed] until requested
 *   again.
 */
internal class OfflineDownloadQueue(
  private val store: OfflineDownloadStore,
  private val fetcher: FileFetcher,
  private val currentUser: () -> User?,
  private val scope: CoroutineScope,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
  private val clock: () -> Long = System::currentTimeMillis,
  maxParallelDownloads: Int = DEFAULT_PARALLEL_DOWNLOADS,
  private val retryDelays: List<Duration> = DEFAULT_RETRY_DELAYS,
  private val progressInterval: Duration = 250.milliseconds,
) : OfflineDownloadManager {

  private data class Entry(
    val manifest: DownloadManifest,
    val files: Map<Int, FileProgress>,
  ) {
    val download: OfflineDownload get() = manifest.toOfflineDownload(files)
    val isFinished: Boolean get() = files.values.all { it.status == FileProgress.Status.Completed }
    val hasFailed: Boolean get() = files.values.any { it.status == FileProgress.Status.Failed }
  }

  private val entries = MutableStateFlow<Map<OfflineDownloadKey, Entry>>(emptyMap())
  private val jobs = ConcurrentHashMap<OfflineDownloadKey, Job>()
  private val permits = Semaphore(maxParallelDownloads)

  private val commands = Channel<suspend () -> Unit>(Channel.UNLIMITED)
  private val commandProcessor = lazy {
    scope.launch(ioDispatcher) {
      for (command in commands) {
        try {
          command()
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          ebark(e) { "Offline download command failed" }
        }
      }
    }
  }

  // Manifests are read on first use rather than at construction, which happens on whatever thread
  // first injects this. getForItem is synchronous, so the read can't be deferred to a coroutine.
  private val loader = lazy {
    val manifests = store.readAll()
    ibark { "Loaded ${manifests.size} offline download(s) from ${store.root}" }
    entries.value = manifests.associate { manifest -> manifest.key to Entry(manifest, progressOnDisk(manifest)) }
  }

  private fun state(): Map<OfflineDownloadKey, Entry> {
    loader.value
    return entries.value
  }

  private fun <T> observe(transform: (Map<OfflineDownloadKey, Entry>) -> T): Flow<T> = flow {
    loader.value
    emitAll(entries.map(transform))
  }.distinctUntilChanged()

  override fun observeAll(): Flow<List<OfflineDownload>> = observe { state ->
    state.values.map { it.download }
  }

  override fun observeForItem(item: LibraryItem): Flow<OfflineDownload> = observe { state ->
    state.downloadFor(OfflineDownloadKey(item.id))
  }

  override fun getForItem(item: LibraryItem): OfflineDownload = state().downloadFor(OfflineDownloadKey(item.id))

  override fun observeForItems(items: List<LibraryItem>): Flow<Map<LibraryItemId, OfflineDownload>> =
    observe { state ->
      items.associate { item -> item.id to state.downloadFor(OfflineDownloadKey(item.id)) }
    }

  override fun observeForEpisode(item: LibraryItem, episode: PodcastEpisode): Flow<OfflineDownload> =
    observe { state ->
      state.downloadFor(OfflineDownloadKey(item.id, episode.id))
    }

  override fun observeForEpisodes(
    item: LibraryItem,
    episodes: List<PodcastEpisode>,
  ): Flow<Map<PodcastEpisodeId, OfflineDownload>> = observe { state ->
    episodes.associate { episode -> episode.id to state.downloadFor(OfflineDownloadKey(item.id, episode.id)) }
  }

  override fun download(item: LibraryItem) {
    enqueue(
      key = OfflineDownloadKey(item.id),
      title = item.media.metadata.title.orEmpty(),
      subtitle = item.media.metadata.authorName.orEmpty(),
      tracks = item.media.tracks,
    )
  }

  override fun downloadAll(items: List<LibraryItem>) {
    items.forEach(::download)
  }

  override fun downloadEpisode(item: LibraryItem, episode: PodcastEpisode) {
    val track = episode.audioTrack ?: run {
      wbark { "Cannot download episode ${episode.id.loggableId}: missing audio track" }
      return
    }
    enqueue(
      key = OfflineDownloadKey(item.id, episode.id),
      title = episode.title,
      subtitle = item.media.metadata.title.orEmpty(),
      tracks = listOf(track),
    )
  }

  override fun delete(item: LibraryItem) = remove(OfflineDownloadKey(item.id))

  override fun deleteEpisode(item: LibraryItem, episode: PodcastEpisode) =
    remove(OfflineDownloadKey(item.id, episode.id))

  override suspend fun deleteAllForItemId(itemId: LibraryItemId) {
    state().keys
      .filter { it.libraryItemId == itemId }
      .forEach(::remove)
  }

  override fun stop(item: LibraryItem) = delete(item)

  override fun stopEpisode(item: LibraryItem, episode: PodcastEpisode) = deleteEpisode(item, episode)

  override fun resumeDownloads() {
    loader.value
    submit {
      entries.value.values
        .filter { entry -> !entry.isFinished && !entry.hasFailed && jobs[entry.manifest.key]?.isActive != true }
        .forEach { entry -> start(entry.manifest) }
    }
  }

  /**
   * The absolute path of the fully downloaded copy of [track], or null when it should stream.
   * Checked against the disk on every call, so a file removed underneath the app streams instead.
   */
  fun localPathFor(libraryItemId: LibraryItemId, episodeId: PodcastEpisodeId?, track: AudioTrack): String? {
    val key = OfflineDownloadKey(libraryItemId, episodeId)
    val file = state()[key]?.manifest?.files?.find { it.trackIndex == track.index } ?: return null
    return store.completedFile(key, file).takeIf { it.isFile }?.absolutePath
  }

  private fun enqueue(key: OfflineDownloadKey, title: String, subtitle: String, tracks: List<AudioTrack>) {
    if (tracks.isEmpty()) {
      wbark { "Nothing to download for ${key.libraryItemId.loggableId}: no audio tracks" }
      return
    }
    val user = currentUser() ?: run {
      wbark { "Cannot download ${key.libraryItemId.loggableId} without a logged in user" }
      return
    }
    val manifest = DownloadManifest(
      libraryItemId = key.libraryItemId,
      episodeId = key.episodeId,
      userId = user.id,
      serverUrl = user.serverUrl,
      title = title,
      subtitle = subtitle,
      createdAtMs = clock(),
      files = tracks.map { track ->
        DownloadManifest.File(
          trackIndex = track.index,
          url = downloadUrl(track.contentUrl),
          expectedBytes = track.metadata.size,
          fileName = OfflineDownloadStore.fileName(track.index, track.metadata.ext),
        )
      },
    )

    loader.value
    var alreadyActive = false
    entries.update { current ->
      alreadyActive = current[key]?.let { it.download.isActive && jobs[key]?.isActive == true } == true
      if (alreadyActive) current else current + (key to Entry(manifest, progressOnDisk(manifest)))
    }
    if (alreadyActive) return

    submit {
      jobs.remove(key)?.cancelAndJoin()
      store.write(manifest)
      start(manifest)
    }
  }

  private fun remove(key: OfflineDownloadKey) {
    loader.value
    entries.update { it - key }
    submit {
      jobs.remove(key)?.cancelAndJoin()
      store.delete(key)
    }
  }

  private fun submit(command: suspend () -> Unit) {
    commandProcessor.value
    commands.trySend(command)
  }

  /** Must run on the command processor. */
  private fun start(manifest: DownloadManifest) {
    val key = manifest.key
    val job = scope.launch(ioDispatcher) {
      permits.withPermit { fetchAll(manifest) }
    }
    jobs[key] = job
    job.invokeOnCompletion { jobs.remove(key, job) }
  }

  private suspend fun fetchAll(manifest: DownloadManifest) {
    val key = manifest.key
    ibark { "Downloading ${manifest.files.size} file(s) for ${key.libraryItemId.loggableId}" }
    for (file in manifest.files) {
      val completed = store.completedFile(key, file)
      if (completed.isFile) {
        updateFile(manifest, file) {
          FileProgress(FileProgress.Status.Completed, completed.length(), completed.length(), clock())
        }
        continue
      }
      if (!fetchFile(manifest, file, completed)) return
    }
    ibark { "Download complete for ${key.libraryItemId.loggableId}" }
  }

  /** Fetches one file with retries. Returns false once it has failed for good. */
  private suspend fun fetchFile(manifest: DownloadManifest, file: DownloadManifest.File, completed: File): Boolean {
    val part = store.partFile(manifest.key, file)
    val request = FetchRequest(
      url = file.url,
      userId = manifest.userId,
      serverUrl = manifest.serverUrl,
      expectedBytes = file.expectedBytes,
    )
    var attempt = 0
    while (true) {
      updateFile(manifest, file) {
        it.copy(status = FileProgress.Status.Downloading, bytes = part.length(), updatedAtMs = clock())
      }
      try {
        var lastPublished = 0L
        fetcher.fetch(request, part) { bytes, total ->
          val now = clock()
          if (now - lastPublished >= progressInterval.inWholeMilliseconds) {
            lastPublished = now
            updateFile(manifest, file) { FileProgress(FileProgress.Status.Downloading, bytes, total, now) }
          }
        }
        moveIntoPlace(part, completed)
        updateFile(manifest, file) {
          FileProgress(FileProgress.Status.Completed, completed.length(), completed.length(), clock())
        }
        return true
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        // A cancelled download surfaces as a closed stream; let the cancellation win
        currentCoroutineContext().ensureActive()
        // Only I/O is worth retrying; anything else (a malformed URL, say) won't change
        val retryable = e is IOException && e !is PermanentFetchException
        if (!retryable || attempt >= retryDelays.size) {
          ebark(e) { "Download of track ${file.trackIndex} for ${manifest.libraryItemId.loggableId} failed" }
          updateFile(manifest, file) {
            it.copy(status = FileProgress.Status.Failed, bytes = part.length(), updatedAtMs = clock())
          }
          return false
        }
        wbark(e) { "Download of track ${file.trackIndex} interrupted, retrying (attempt ${attempt + 1})" }
        delay(retryDelays[attempt++])
      }
    }
  }

  /** Applies [transform] to [file]'s progress, unless [manifest]'s download was since removed or replaced. */
  private fun updateFile(
    manifest: DownloadManifest,
    file: DownloadManifest.File,
    transform: (FileProgress) -> FileProgress,
  ) {
    entries.update { current ->
      val entry = current[manifest.key]?.takeIf { it.manifest === manifest } ?: return@update current
      val previous = entry.files[file.trackIndex] ?: FileProgress(FileProgress.Status.Queued)
      current + (manifest.key to entry.copy(files = entry.files + (file.trackIndex to transform(previous))))
    }
  }

  private fun progressOnDisk(manifest: DownloadManifest): Map<Int, FileProgress> =
    manifest.files.associate { file ->
      val completed = store.completedFile(manifest.key, file)
      file.trackIndex to if (completed.isFile) {
        FileProgress(FileProgress.Status.Completed, completed.length(), completed.length(), completed.lastModified())
      } else {
        FileProgress(FileProgress.Status.Queued, store.partFile(manifest.key, file).length())
      }
    }

  private fun Map<OfflineDownloadKey, Entry>.downloadFor(key: OfflineDownloadKey): OfflineDownload =
    this[key]?.download ?: OfflineDownload(libraryItemId = key.libraryItemId, episodeId = key.episodeId)

  private fun moveIntoPlace(part: File, target: File) {
    try {
      Files.move(part.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    } catch (_: AtomicMoveNotSupportedException) {
      Files.move(part.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
  }

  companion object : Cork {
    override val tag: String = "OfflineDownloads"
    override val enabled: Boolean = true

    const val DEFAULT_PARALLEL_DOWNLOADS = 2
    val DEFAULT_RETRY_DELAYS = listOf(2.seconds, 10.seconds, 30.seconds)

    private val LIBRARY_FILE_PATH = Regex("/api/items/[^/?#]+/file/[^/?#]+$")

    /**
     * The download route for a track's streaming [contentUrl]: ABS serves the same file from
     * `/api/items/:id/file/:ino/download`, which, unlike the streaming route, enforces the user's
     * download permission — the route the official apps download through. Any other URL shape is
     * downloaded as-is.
     */
    fun downloadUrl(contentUrl: String): String {
      val path = contentUrl.substringBefore('?').substringBefore('#')
      return if (LIBRARY_FILE_PATH.containsMatchIn(path)) "$path/download" else contentUrl
    }
  }
}

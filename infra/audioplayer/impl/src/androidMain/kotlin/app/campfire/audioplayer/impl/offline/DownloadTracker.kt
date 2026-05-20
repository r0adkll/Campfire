package app.campfire.audioplayer.impl.offline

import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.audioplayer.offline.OfflineDownloadKey
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.PodcastEpisode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

/**
 * Helper class to track and cache downloads from [DownloadManager]
 */
@OptIn(UnstableApi::class)
@SingleIn(AppScope::class)
@Inject
class DownloadTracker(
  private val downloadManager: DownloadManager,
  @ForScope(AppScope::class) private val scope: CoroutineScope,
) : DownloadManager.Listener {

  sealed interface Event {
    data object Loaded : Event
    data class Changed(val download: Download) : Event
    data class Removed(val download: Download) : Event
  }

  private val downloads = mutableMapOf<Uri, Download>()

  private val events = MutableSharedFlow<Event>(
    replay = 1,
    extraBufferCapacity = 8,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
  )

  init {
    downloadManager.addListener(this)
    loadAllDownloads()
  }

  fun observeEvents(): SharedFlow<Event> = events.asSharedFlow()

  @kotlin.OptIn(ExperimentalCoroutinesApi::class)
  fun observeDownloads(): Flow<List<OfflineDownload>> = events
    .mapLatest {
      val keys = downloads.values
        .map { OfflineDownloadKey.decode(it.request.data) }
        .toSet()

      keys.map { key ->
        getOfflineDownload(key)
      }
    }

  fun getOfflineDownload(item: LibraryItem): OfflineDownload {
    val itemDownloads = if (item.media.tracks.isNotEmpty()) {
      item.media.tracks.mapNotNull {
        downloads[it.contentUrl.toUri()]
      }
    } else {
      // Find all the downloads by the items associated metadata id. Restrict to
      // book-level entries (episodeId == null) so a podcast's item-level state isn't
      // polluted by its per-episode downloads.
      downloads.values
        .filter {
          val decoded = OfflineDownloadKey.decode(it.request.data)
          decoded.libraryItemId == item.id && decoded.episodeId == null
        }
    }

    // If we don't have any active downloads for an item, just return the default
    if (itemDownloads.isEmpty()) return OfflineDownload(item.id)

    return computeOfflineDownload(
      key = OfflineDownloadKey(item.id),
      itemDownloads = itemDownloads,
    )
  }

  fun getOfflineDownload(item: LibraryItem, episode: PodcastEpisode): OfflineDownload {
    return getOfflineDownload(OfflineDownloadKey(item.id, episode.id))
  }

  fun getOfflineDownload(key: OfflineDownloadKey): OfflineDownload {
    val itemDownloads = downloads.values
      .filter { OfflineDownloadKey.decode(it.request.data) == key }

    if (itemDownloads.isEmpty()) {
      return OfflineDownload(libraryItemId = key.libraryItemId, episodeId = key.episodeId)
    }

    return computeOfflineDownload(
      key = key,
      itemDownloads = itemDownloads,
    )
  }

  private fun computeOfflineDownload(
    key: OfflineDownloadKey,
    itemDownloads: List<Download>,
  ): OfflineDownload {
    // Condense the collective set of states
    val states = itemDownloads
      .map { it.state }
      .distinct()

    val startTimeMs = itemDownloads.minOf { it.startTimeMs }
    val updateTimeMs = itemDownloads.maxOf { it.updateTimeMs }
    val contentLength = itemDownloads.sumOf { it.contentLength }

    var allDownloadPercentagesUnknown = true
    var haveDownloadingTasks = false
    var downloadTaskCount = 0
    var totalPercentage = 0f
    var downloadedBytes = 0L
    var totalContentLength = 0L
    itemDownloads.forEach { download ->
      when (download.state) {
        Download.STATE_DOWNLOADING -> {
          haveDownloadingTasks = true
          if (download.percentDownloaded != C.PERCENTAGE_UNSET.toFloat()) {
            allDownloadPercentagesUnknown = false
            totalPercentage += download.percentDownloaded
          }
          downloadTaskCount++
        }
        else -> Unit
      }

      downloadedBytes += download.bytesDownloaded
      totalContentLength += download.contentLength
    }

    val currentProgress = (totalPercentage / downloadTaskCount)
    val isIndeterminate = !haveDownloadingTasks ||
      contentLength <= 0L ||
      (allDownloadPercentagesUnknown && downloadedBytes > 0L)

    return OfflineDownload(
      libraryItemId = key.libraryItemId,
      episodeId = key.episodeId,
      state = when {
        states.contains(Download.STATE_FAILED) -> OfflineDownload.State.Failed
        states.contains(Download.STATE_STOPPED) -> OfflineDownload.State.Stopped
        states.contains(Download.STATE_DOWNLOADING) -> OfflineDownload.State.Downloading
        states.contains(Download.STATE_QUEUED) -> OfflineDownload.State.Queued
        states.isNotEmpty() && states.all { it == Download.STATE_COMPLETED } -> OfflineDownload.State.Completed
        else -> OfflineDownload.State.None
      },
      startTimeMs = startTimeMs,
      updateTimeMs = updateTimeMs,
      contentLength = contentLength,
      progress = OfflineDownload.Progress(
        bytes = downloadedBytes,
        percent = (currentProgress / 100f).coerceIn(0f, 1f),
        indeterminate = isIndeterminate,
      ),
    )
  }

  override fun onDownloadChanged(downloadManager: DownloadManager, download: Download, finalException: Exception?) {
    downloads[download.request.uri] = download
    scope.launch { events.emit(Event.Changed(download)) }
  }

  override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
    downloads.remove(download.request.uri)
    scope.launch { events.emit(Event.Removed(download)) }
  }

  private fun loadAllDownloads() = scope.launch {
    try {
      downloadManager.downloadIndex.getDownloads().use { cursor ->
        while (cursor.moveToNext()) {
          val download = cursor.download
          downloads[download.request.uri] = download
        }
      }
    } catch (e: Exception) {
      bark(LogPriority.ERROR, throwable = e) { "Failed to load downloads from DownloadManager" }
    } finally {
      events.emit(Event.Loaded)
    }
  }
}

/**
 * A [Download.State] that represents a null download for a given media item uri
 */
private const val STATE_NONE = -1

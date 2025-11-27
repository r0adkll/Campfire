package app.campfire.audioplayer.offline

import app.campfire.audioplayer.offline.OfflineDownload.State.Completed
import app.campfire.audioplayer.offline.OfflineDownload.State.Downloading
import app.campfire.audioplayer.offline.OfflineDownload.State.Failed
import app.campfire.audioplayer.offline.OfflineDownload.State.None
import app.campfire.audioplayer.offline.OfflineDownload.State.Queued
import app.campfire.audioplayer.offline.OfflineDownload.State.Stopped
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.offline.OfflineStatus
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun OfflineDownload?.isNullOrNone(): Boolean {
  contract {
    returns(false) implies (this@isNullOrNone != null)
  }
  return this == null || state == None
}

/**
 * Represents the download state of a [LibraryItem].
 */
data class OfflineDownload(
  val libraryItemId: LibraryItemId,
  val state: State = None,
  val startTimeMs: Long = -1L,
  val updateTimeMs: Long = -1L,
  val contentLength: Long = -1L,
  val progress: Progress = Progress(0L, 0f),
) {

  val isCompleted: Boolean
    get() = state == Completed

  val isActive: Boolean
    get() = state != Completed && state != None

  enum class State {
    /**
     * This item is not downloaded for offline use at all. It may
     * be cached depending on the platform.
     */
    None,

    /**
     * This item is queued for download.
     */
    Queued,

    /**
     * This download has been stopped.
     */
    Stopped,

    /**
     * This item is actively downloading. [progress] should
     * contain the status.
     */
    Downloading,

    /**
     * This item is fully downloaded for offline use.
     */
    Completed,

    /**
     * This item failed to download.
     */
    Failed,
  }

  data class Progress(
    val bytes: Long,
    val percent: Float,
    val indeterminate: Boolean = false,
  )
}

/**
 * Converts [OfflineDownload] into a UI friendly model for rendering
 */
fun OfflineDownload?.asWidgetStatus(): OfflineStatus = when {
  this == null -> OfflineStatus.None
  else -> when (state) {
    None -> OfflineStatus.None
    Queued -> OfflineStatus.Queued
    Downloading -> OfflineStatus.Downloading(progress.percent)
    Completed -> OfflineStatus.Available
    Stopped,
    Failed,
    -> OfflineStatus.Failed
  }
}

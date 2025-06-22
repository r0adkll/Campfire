package app.campfire.audioplayer.offline

import app.campfire.core.model.LibraryItem

/**
 * Represents the download state of a [LibraryItem].
 */
data class OfflineDownload(
  val libraryItem: LibraryItem,
  val state: State = State.None,
  val startTimeMs: Long = -1L,
  val updateTimeMs: Long = -1L,
  val contentLength: Long = -1L,
  val progress: Progress = Progress(0L, 0f),
) {

  val isCompleted: Boolean
    get() = state == State.Completed

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

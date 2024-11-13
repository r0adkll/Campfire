package app.campfire.core.model

import app.campfire.core.extensions.seconds

typealias LibraryItemId = String

data class LibraryItem(
  val id: LibraryItemId,
  val libraryId: LibraryId,
  val isMissing: Boolean,
  val isInvalid: Boolean,
  val mediaType: MediaType,
  val numFiles: Int,
  val sizeInBytes: Long,
  val addedAtMillis: Long,
  val updatedAtMillis: Long,
  val media: Media,

  val libraryFiles: List<LibraryFile> = emptyList(),
  val userMediaProgress: MediaProgress? = null,
) {

  /**
   * Get the current chapter for the total duration of time passed in the
   * playback
   *
   * @param durationMs the cumulative duration of the playback for the current library item
   */
  fun getChapterForDuration(durationMs: Long): Chapter {
    return media.chapters.find {
      val startMs = it.start.seconds.inWholeMilliseconds
      val endMs = it.end.seconds.inWholeMilliseconds
      durationMs in startMs..<endMs
    } ?: error("Unable to find chapter for duration $durationMs")
  }
}

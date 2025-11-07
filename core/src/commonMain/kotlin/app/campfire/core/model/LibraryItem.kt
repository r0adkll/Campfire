package app.campfire.core.model

import app.campfire.core.extensions.seconds

typealias LibraryItemId = String

data class LibraryItem(
  val id: LibraryItemId,
  val ino: String,
  val libraryId: LibraryId,
  val oldLibraryId: String? = null,
  val folderId: String,
  val path: String,
  val relPath: String,
  val isFile: Boolean,
  val mtimeMs: Long,
  val ctimeMs: Long,
  val birthtimeMs: Long,
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
) : ShelfEntity {

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

/**
 * Take only the last 5 characters of an ID so that way we can log and associate without
 * exposing local user data.
 */
val LibraryItemId.loggableId: String
  get() = takeLast(5)

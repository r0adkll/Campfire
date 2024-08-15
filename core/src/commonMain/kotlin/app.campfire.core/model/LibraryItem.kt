package app.campfire.core.model

typealias LibraryItemId = String

data class LibraryItem(
  val id: LibraryItemId,
  val libraryId: LibraryId,
  val isMissing: Boolean,
  val isInvalid: Boolean,
  val mediaType: MediaType,
  val numFiles: Int,
  val sizeInBytes: Long,
  val media: MediaMinified,
)

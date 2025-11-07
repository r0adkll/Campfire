package app.campfire.core.model

data class Folder(
  val id: String,
  val fullPath: String,
  val libraryId: String,
  val addedAt: Long,
)

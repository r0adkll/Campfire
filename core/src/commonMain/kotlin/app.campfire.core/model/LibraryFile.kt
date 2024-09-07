package app.campfire.core.model

data class LibraryFile(
  val ino: String,
  val metadata: FileMetadata,
  val isSupplementary: Boolean? = null,
  val addedAt: Long,
  val updatedAt: Long,
  val fileType: String,
)

package app.campfire.core.model

data class FileMetadata(
  val filename: String,
  val ext: String,
  val path: String,
  val relPath: String,
  val size: Int,
  val mtimeMs: Long,
  val ctimeMs: Long,
  val birthtimeMs: Long,
)

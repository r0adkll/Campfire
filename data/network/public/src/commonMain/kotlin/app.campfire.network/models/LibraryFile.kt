package app.campfire.network.models

import kotlinx.serialization.Serializable

@Serializable
data class LibraryFile(
  val ino: String,
  val metadata: FileMetadata,
  val isSupplementary: Boolean? = null,
  val addedAt: Long,
  val updatedAt: Long,
  val fileType: String,
)

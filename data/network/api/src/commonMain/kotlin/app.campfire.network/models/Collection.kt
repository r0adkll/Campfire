package app.campfire.network.models

import kotlinx.serialization.Serializable

@Serializable
data class Collection(
  val id: String,
  val libraryId: String,
  val name: String,
  val description: String?,
  val cover: String? = null,
  val coverFullPath: String? = null,
  val books: List<LibraryItemMinified<ExpandedBookMetadata>>,
  val lastUpdate: Long,
  val createdAt: Long,
)

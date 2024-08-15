package app.campfire.core.model

typealias LibraryId = String

data class Library(
  val id: LibraryId,
  val name: String,
  val folders: List<Folder>,
  val displayOrder: Int,
  val icon: String,
  val mediaType: String,
  val provider: String,
  val settings: LibrarySettings,
  val createdAt: Long,
  val lastUpdate: Long,
)

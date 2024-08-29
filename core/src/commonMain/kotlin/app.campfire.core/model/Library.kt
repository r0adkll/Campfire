package app.campfire.core.model

typealias LibraryId = String

data class Library(
  val id: LibraryId,
  val name: String,
  val displayOrder: Int,
  val icon: String,
  val mediaType: String,
  val provider: String,
  val coverAspectRatio: Int,
  val audiobooksOnly: Boolean,
  val createdAt: Long,
  val lastUpdate: Long,
)

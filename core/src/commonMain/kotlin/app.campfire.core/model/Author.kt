package app.campfire.core.model

typealias AuthorId = String

data class Author(
  val id: AuthorId,
  val asin: String?,
  val name: String,
  val description: String?,
  val imagePath: String?,
  val addedAt: Long,
  val updatedAt: Long,
  val numBooks: Int?,

  val libraryItems: List<LibraryItem> = emptyList(),
  val series: List<Series> = emptyList(),
) : ShelfEntity

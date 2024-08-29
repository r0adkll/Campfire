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

  val libraryItems: List<Library>? = null,
  val series: List<Series>? = null,
)

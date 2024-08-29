package app.campfire.network.models

import kotlinx.serialization.Serializable

/**
 * The minified metadata for a book in the database.
 *
 * @param title The title of the book. Will be null if unknown.
 * @param subtitle The subtitle of the book. Will be null if there is no subtitle.
 * @param genres The genres of the book.
 * @param publishedYear The year the book was published. Will be null if unknown.
 * @param publishedDate The date the book was published. Will be null if unknown.
 * @param publisher The publisher of the book. Will be null if unknown.
 * @param description A description for the book. Will be null if empty.
 * @param isbn The ISBN of the book. Will be null if unknown.
 * @param asin The ASIN of the book. Will be null if unknown.
 * @param language The language of the book. Will be null if unknown.
 * @param explicit Whether the book has been marked as explicit.
 * @param titleIgnorePrefix The title of the book with any prefix moved to the end.
 * @param authorName The name of the book's author(s).
 * @param authorNameLF The name of the book's author(s) with last names first.
 * @param narratorName The name of the audiobook's narrator(s).
 * @param seriesName The name of the book's series.
 */
@Serializable
data class BookMetadataMinified(
  val title: String? = null,
  val subtitle: String? = null,
  val genres: List<String>? = null,
  val publishedYear: String? = null,
  val publishedDate: String? = null,
  val publisher: String? = null,
  val description: String? = null,
  val isbn: String? = null,
  val asin: String? = null,
  val language: String? = null,
  val explicit: Boolean = false,
  val abridged: Boolean = false,
  val titleIgnorePrefix: String? = null,
  val authorName: String? = null,
  val authorNameLF: String? = null,
  val narratorName: String? = null,
  val seriesName: String? = null,

  /**
   * When [LibraryItemMinified] is returned from the personalized library endpoint,
   * and its shelf has an id of `continue-series` then this field will be non-null.
   *
   * The sequence in a series that this book is of
   */
  val series: SeriesSequence? = null,
)

@Serializable
data class SeriesSequence(
  val id: String,
  val name: String,
  val sequence: Int,
)

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
abstract class BookMetadata {
  abstract val title: String?
  abstract val subtitle: String?
  abstract val genres: List<String>?
  abstract val publishedYear: String?
  abstract val publishedDate: String?
  abstract val publisher: String?
  abstract val description: String?
  abstract val isbn: String?
  abstract val asin: String?
  abstract val language: String?
  abstract val explicit: Boolean
  abstract val abridged: Boolean
  abstract val titleIgnorePrefix: String?
  abstract val authorName: String?
  abstract val authorNameLF: String?
  abstract val narratorName: String?
  abstract val seriesName: String?
}

@Serializable
data class MinifiedBookMetadata(
  override val title: String? = null,
  override val subtitle: String? = null,
  override val genres: List<String>? = null,
  override val publishedYear: String? = null,
  override val publishedDate: String? = null,
  override val publisher: String? = null,
  override val description: String? = null,
  override val isbn: String? = null,
  override val asin: String? = null,
  override val language: String? = null,
  override val explicit: Boolean = false,
  override val abridged: Boolean = false,
  override val titleIgnorePrefix: String? = null,
  override val authorName: String? = null,
  override val authorNameLF: String? = null,
  override val narratorName: String? = null,
  override val seriesName: String? = null,

  /**
   * When [LibraryItemMinified] is returned from the personalized library endpoint,
   * and its shelf has an id of `continue-series` then this field will be non-null.
   *
   * The sequence in a series that this book is of
   */
  val series: SeriesSequence? = null,
) : BookMetadata()

@Serializable
data class ExpandedBookMetadata(
  override val title: String? = null,
  override val subtitle: String? = null,
  override val genres: List<String>? = null,
  override val publishedYear: String? = null,
  override val publishedDate: String? = null,
  override val publisher: String? = null,
  override val description: String? = null,
  override val isbn: String? = null,
  override val asin: String? = null,
  override val language: String? = null,
  override val explicit: Boolean = false,
  override val abridged: Boolean = false,

  /**
   * Minified Fields
   */
  override val titleIgnorePrefix: String? = null,
  override val authorName: String? = null,
  override val authorNameLF: String? = null,
  override val narratorName: String? = null,
  override val seriesName: String? = null,

  /**
   * List of authors in the meta
   */
  val authors: List<AuthorSeries>? = null,

  /**
   * List of narrators in the meta
   */
  val narrators: List<String>? = null,

  /**
   * When [LibraryItemMinified] is returned from the collections endpoint, its metadata
   * will contain this
   */
  val series: List<SeriesSequence>? = null,
) : BookMetadata()

@Serializable
data class SeriesSequence(
  val id: String,
  val name: String,
  val sequence: Int,
)

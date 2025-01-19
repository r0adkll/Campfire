package app.campfire.network.models

import kotlinx.serialization.Serializable

@Serializable
data class SearchResult(
  val book: List<LibraryItemSearchResult>,
  val narrators: List<NarratorSearchResult>,
  val authors: List<Author>,
  val tags: List<TagSearchResult>,
  val genres: List<TagSearchResult>,
  val series: List<SeriesSearchResult>,
) : NetworkModel() {

  fun toShortString(): String {
    return "SearchResult(books=${book.size}, narrators=${narrators.size}, authors=${authors.size}, " +
      "tags=${tags.size}, genres=${genres.size}, series=${series.size})"
  }
}

@Serializable
data class LibraryItemSearchResult(
  val libraryItem: LibraryItemExpanded,
  val matchKey: String? = null,
  val matchText: String? = null,
)

@Serializable
data class SeriesSearchResult(
  val series: Series,
  val books: List<LibraryItemExpanded>,
)

@Serializable
data class NarratorSearchResult(
  val name: String,
  val numBooks: Int,
)

@Serializable
data class TagSearchResult(
  val name: String,
  val numItems: Int,
)

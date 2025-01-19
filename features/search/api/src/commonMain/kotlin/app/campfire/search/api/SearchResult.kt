package app.campfire.search.api

import app.campfire.core.model.Author
import app.campfire.core.model.BasicSearchResult
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Series

sealed interface SearchResult {
  data object Loading : SearchResult
  data object Error : SearchResult
  data class Success(
    val books: List<LibraryItem>,
    val narrators: List<BasicSearchResult>,
    val authors: List<Author>,
    val series: List<Series>,
    val tags: List<BasicSearchResult>,
    val genres: List<BasicSearchResult>,
  ) : SearchResult {
    val isEmpty: Boolean get() = books.isEmpty() &&
      narrators.isEmpty() &&
      authors.isEmpty() &&
      series.isEmpty() &&
      tags.isEmpty() &&
      genres.isEmpty()

    val isNotEmpty: Boolean get() = !isEmpty
  }

  companion object {
    val Empty get() = Success(
      books = emptyList(),
      narrators = emptyList(),
      authors = emptyList(),
      series = emptyList(),
      tags = emptyList(),
      genres = emptyList(),
    )
  }
}

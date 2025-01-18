package app.campfire.search.api

import app.campfire.core.model.Author
import app.campfire.core.model.BasicSearchResult
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Series

sealed interface SearchResult {
  data object Loading : SearchResult
  data class Success(
    val books: List<LibraryItem>,
    val narrators: List<BasicSearchResult>,
    val authors: List<Author>,
    val series: List<Series>,
    val tags: List<BasicSearchResult>,
    val genres: List<BasicSearchResult>,
  ) : SearchResult
  data object Error : SearchResult
}

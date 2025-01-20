package app.campfire.search.ui

import app.campfire.core.model.Author
import app.campfire.core.model.BasicSearchResult
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Series
import app.campfire.search.api.SearchResult
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class SearchUiState(
  val query: String,
  val searchResult: SearchResult,
  val eventSink: (SearchUiEvent) -> Unit,
) : CircuitUiState

sealed interface SearchUiEvent : CircuitUiEvent {
  data class QueryChanged(val query: String) : SearchUiEvent
  data object ClearQuery : SearchUiEvent
  data object Dismiss : SearchUiEvent

  data class OnBookClick(val book: LibraryItem) : SearchUiEvent
  data class OnNarratorClick(val narrator: BasicSearchResult) : SearchUiEvent
  data class OnAuthorClick(val author: Author) : SearchUiEvent
  data class OnSeriesClick(val series: Series) : SearchUiEvent
  data class OnTagClick(val tag: BasicSearchResult) : SearchUiEvent
  data class OnGenreClick(val genre: BasicSearchResult) : SearchUiEvent
}

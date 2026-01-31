package app.campfire.author.ui.list

import androidx.compose.runtime.Stable
import androidx.paging.compose.LazyPagingItems
import app.campfire.core.model.Author
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.SortDirection
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Stable
data class AuthorsUiState(
  val numAuthors: Int,
  val lazyPagingItems: LazyPagingItems<Author>,
  val sortMode: ContentSortMode,
  val sortDirection: SortDirection,
  val eventSink: (AuthorsUiEvent) -> Unit,
) : CircuitUiState

sealed interface AuthorsUiEvent : CircuitUiEvent {
  data class AuthorClick(val author: Author) : AuthorsUiEvent
  data class SortModeSelected(val mode: ContentSortMode) : AuthorsUiEvent
}

package app.campfire.author.ui.detail

import app.campfire.core.model.Author
import app.campfire.core.model.LibraryItem
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class AuthorDetailUiState(
  val authorContentState: AuthorContentState,
  val eventSink: (AuthorDetailUiEvent) -> Unit,
) : CircuitUiState

sealed interface AuthorContentState {
  data object Loading : AuthorContentState
  data class Loaded(val author: Author) : AuthorContentState
  data object Error : AuthorContentState
}

sealed interface AuthorDetailUiEvent : CircuitUiEvent {
  data object Back : AuthorDetailUiEvent
  data class LibraryItemClick(val libraryItem: LibraryItem) : AuthorDetailUiEvent
}

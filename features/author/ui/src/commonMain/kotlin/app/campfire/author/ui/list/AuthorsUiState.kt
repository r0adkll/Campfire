package app.campfire.author.ui.list

import app.campfire.core.model.Author
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class AuthorsUiState(
  val authorContentState: AuthorsContentState,
  val eventSink: (AuthorsUiEvent) -> Unit,
) : CircuitUiState

sealed interface AuthorsContentState {
  data object Loading : AuthorsContentState
  data class Loaded(val authors: List<Author>) : AuthorsContentState
  data object Error : AuthorsContentState
}

sealed interface AuthorsUiEvent : CircuitUiEvent {
  data class AuthorClick(val author: Author) : AuthorsUiEvent
}

package app.campfire.author.ui.list

import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.Author
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class AuthorsUiState(
  val authorContentState: LoadState<out List<Author>>,
  val eventSink: (AuthorsUiEvent) -> Unit,
) : CircuitUiState

sealed interface AuthorsUiEvent : CircuitUiEvent {
  data class AuthorClick(val author: Author) : AuthorsUiEvent
}

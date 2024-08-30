package app.campfire.libraries.ui.list

import app.campfire.core.model.LibraryItem
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class LibraryUiState(
  val contentState: LibraryContentState,
  val eventSink: (LibraryUiEvent) -> Unit,
) : CircuitUiState

sealed interface LibraryContentState {
  data object Loading : LibraryContentState
  data class Loaded(val items: List<LibraryItem>) : LibraryContentState
  data object Error : LibraryContentState

  fun map(mapper: (List<LibraryItem>) -> List<LibraryItem>): LibraryContentState = when (this) {
    Loading -> this
    Error -> this
    is Loaded -> Loaded(mapper(items))
  }
}

sealed interface LibraryUiEvent : CircuitUiEvent {
  data object OpenDrawer : LibraryUiEvent
  data object OpenSearch : LibraryUiEvent
}

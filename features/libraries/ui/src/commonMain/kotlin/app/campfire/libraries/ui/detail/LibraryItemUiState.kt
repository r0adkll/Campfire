package app.campfire.libraries.ui.detail

import app.campfire.core.model.LibraryItem
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class LibraryItemUiState(
  val libraryItemContentState: LibraryItemContentState,
  val eventSink: (LibraryItemUiEvent) -> Unit,
) : CircuitUiState

sealed interface LibraryItemContentState {
  data object Loading : LibraryItemContentState
  data class Loaded(val item: LibraryItem) : LibraryItemContentState
  data object Error : LibraryItemContentState
}

sealed interface LibraryItemUiEvent : CircuitUiEvent {
  data object OnBack : LibraryItemUiEvent
}

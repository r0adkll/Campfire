package app.campfire.libraries.ui.detail

import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Session
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class LibraryItemUiState(
  val sessionUiState: SessionUiState,
  val libraryItemContentState: LibraryItemContentState,
  val eventSink: (LibraryItemUiEvent) -> Unit,
) : CircuitUiState

sealed interface SessionUiState {
  data object None : SessionUiState
  data class Current(val session: Session) : SessionUiState
}

sealed interface LibraryItemContentState {
  data object Loading : LibraryItemContentState
  data class Loaded(val item: LibraryItem) : LibraryItemContentState
  data object Error : LibraryItemContentState
}

sealed interface LibraryItemUiEvent : CircuitUiEvent {
  data class PlayClick(val item: LibraryItem) : LibraryItemUiEvent
  data object OnBack : LibraryItemUiEvent
}

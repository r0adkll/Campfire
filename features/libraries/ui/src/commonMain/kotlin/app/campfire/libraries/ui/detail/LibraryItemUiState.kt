package app.campfire.libraries.ui.detail

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class LibraryItemUiState(
  val eventSink: (LibraryItemUiEvent) -> Unit,
) : CircuitUiState

sealed interface LibraryItemUiEvent : CircuitUiEvent

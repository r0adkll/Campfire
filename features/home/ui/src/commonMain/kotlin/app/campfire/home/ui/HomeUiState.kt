package app.campfire.home.ui

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class HomeUiState(
  val eventSink: (HomeUiEvent) -> Unit,
) : CircuitUiState

sealed interface HomeUiEvent : CircuitUiEvent {

}

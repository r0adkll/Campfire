package app.campfire.auth.ui.welcome

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

class WelcomeUiState(
  val eventSink: (WelcomeUiEvent) -> Unit,
) : CircuitUiState

sealed interface WelcomeUiEvent : CircuitUiEvent {
  data object AddCampsite : WelcomeUiEvent
}

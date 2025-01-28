package app.campfire.auth.ui.welcome

import app.campfire.auth.ui.login.LoginUiState
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

class WelcomeUiState(
  val loginUiState: LoginUiState,
  val eventSink: (WelcomeUiEvent) -> Unit,
) : CircuitUiState

sealed interface WelcomeUiEvent : CircuitUiEvent {
  data object AddCampsite : WelcomeUiEvent
}

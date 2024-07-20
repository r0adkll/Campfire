package app.campfire.auth.ui.login

import androidx.compose.runtime.Stable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Stable
data class LoginUiState(
  val eventSink: (LoginUiEvent) -> Unit,
) : CircuitUiState

sealed interface LoginUiEvent : CircuitUiEvent

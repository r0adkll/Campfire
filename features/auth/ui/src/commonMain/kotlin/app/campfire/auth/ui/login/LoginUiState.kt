package app.campfire.auth.ui.login

import androidx.compose.runtime.Stable
import app.campfire.core.model.Tent
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Stable
data class LoginUiState(
  val tent: Tent,
  val serverName: String,
  val serverUrl: String,
  val connectionState: ConnectionState?,
  val userName: String,
  val password: String,
  val isAuthenticating: Boolean,
  val authError: AuthError?,
  val eventSink: (LoginUiEvent) -> Unit,
) : CircuitUiState

sealed interface LoginUiEvent : CircuitUiEvent {
  data class ChangeTent(val tent: Tent) : LoginUiEvent
  data class ServerName(val serverName: String) : LoginUiEvent
  data class ServerUrl(val url: String) : LoginUiEvent
  data class UserName(val userName: String) : LoginUiEvent
  data class Password(val password: String) : LoginUiEvent
  data object AddCampsite : LoginUiEvent
}

sealed interface AuthError {
  data object InvalidCredentials : AuthError
  data object NetworkError : AuthError
}

enum class ConnectionState {
  Loading,
  Error,
  Success,
}

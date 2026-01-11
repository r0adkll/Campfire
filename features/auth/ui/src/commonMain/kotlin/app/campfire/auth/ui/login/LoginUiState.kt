package app.campfire.auth.ui.login

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import app.campfire.core.model.NetworkSettings
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
  val networkSettings: NetworkSettings?,

  val eventSink: (LoginUiEvent) -> Unit,
) : CircuitUiState

@Immutable
data class OpenIdUiState(
  val customMessage: String?,
  val buttonText: String?,
)

@Immutable
data class AuthMethodState(
  val passwordAuthEnabled: Boolean,
  val openIdState: OpenIdUiState?,
)

sealed interface LoginUiEvent : CircuitUiEvent {
  data object NavigateBack : LoginUiEvent
  data class ChangeTent(val tent: Tent) : LoginUiEvent
  data class ServerName(val serverName: String) : LoginUiEvent
  data class ServerUrl(val url: String) : LoginUiEvent
  data class UserName(val userName: String) : LoginUiEvent
  data class Password(val password: String) : LoginUiEvent
  data class ChangeNetworkSettings(val settings: NetworkSettings) : LoginUiEvent
  data object AddCampsite : LoginUiEvent
  data object StartOpenIdAuth : LoginUiEvent
}

sealed interface AuthError {
  data object InvalidCredentials : AuthError
  data object NetworkError : AuthError
  data object OAuthError : AuthError
}

sealed interface ConnectionState {
  data object Loading : ConnectionState
  data class Error(val cause: Throwable) : ConnectionState
  data class Success(val authMethodState: AuthMethodState) : ConnectionState
}

// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.login

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import app.campfire.account.api.RestorableAccount
import app.campfire.core.model.NetworkSettings
import app.campfire.ui.theming.api.AppTheme
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Stable
data class LoginUiState(
  val theme: AppTheme.Fixed,
  val serverName: String,
  val serverUrl: String,
  val connectionState: ConnectionState?,
  val userName: String,
  val password: String,
  val isAuthenticating: Boolean,
  val authError: AuthError?,
  val networkSettings: NetworkSettings?,
  val restorableAccounts: List<RestorableAccount> = emptyList(),
  val restoredTheme: AppTheme = AppTheme.Fixed.Tent,
  /**
   * Focus the password field once it appears, instead of the next field. Set while signing back
   * into a restored account, whose other fields are already filled in.
   */
  val focusPassword: Boolean = false,

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
  data class ChangeTheme(val theme: AppTheme.Fixed) : LoginUiEvent
  data class ServerName(val serverName: String) : LoginUiEvent
  data class ServerUrl(val url: String) : LoginUiEvent
  data class UserName(val userName: String) : LoginUiEvent
  data class Password(val password: String) : LoginUiEvent
  data class ChangeNetworkSettings(val settings: NetworkSettings) : LoginUiEvent
  data object AddCampsite : LoginUiEvent
  data object StartOpenIdAuth : LoginUiEvent
  data class SelectRestorableAccount(val account: RestorableAccount) : LoginUiEvent
  data class DismissRestorableAccount(val account: RestorableAccount) : LoginUiEvent
}

sealed interface AuthError {
  data object InvalidCredentials : AuthError
  data object NoLibraryAccess : AuthError
  data object UnexpectedResponse : AuthError
  data object NetworkError : AuthError
  data object OAuthError : AuthError
  data object OAuthInvalidRedirectUri : AuthError
}

sealed interface ConnectionState {
  data object Loading : ConnectionState
  data class Error(val cause: Throwable) : ConnectionState
  data class Success(val authMethodState: AuthMethodState) : ConnectionState
}

package app.campfire.account.ui.picker

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.Server
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Stable
data class AccountPickerUiState(
  val accountState: LoadState<out AccountState>,
  val eventSink: (AccountPickerUiEvent) -> Unit,
) : CircuitUiState

@Immutable
data class AccountState(
  val current: Server,
  val all: List<UiServer>,
)

@Immutable
data class UiServer(
  val server: Server,
  val authState: AuthState,
)

@Immutable
sealed interface AuthState {
  data object Valid : AuthState
  data object NeedsReauthentication : AuthState
}

sealed interface AccountPickerUiEvent : CircuitUiEvent {
  data class Logout(val server: Server) : AccountPickerUiEvent
}

package app.campfire.account.ui.picker

import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.Server
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class AccountPickerUiState(
  val accountState: LoadState<out AccountState>,
  val eventSink: (AccountPickerUiEvent) -> Unit,
) : CircuitUiState

data class AccountState(
  val current: Server,
  val all: List<Server>,
)

sealed interface AccountPickerUiEvent : CircuitUiEvent {
  data class Logout(val server: Server) : AccountPickerUiEvent
}

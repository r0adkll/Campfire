package app.campfire.account.ui.switcher

import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.Library
import app.campfire.core.model.Server
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class AccountSwitcherUiState(
  val currentAccount: LoadState<out Server>,
  val allAccounts: LoadState<out List<Server>>,
  val libraryState: LibraryState?,
  val eventSink: (AccountSwitcherUiEvent) -> Unit,
) : CircuitUiState

data class LibraryState(
  val currentLibrary: Library,
  val allLibraries: LoadState<out List<Library>>,
)

sealed interface AccountSwitcherUiEvent : CircuitUiEvent {
  data class SelectLibrary(val library: Library) : AccountSwitcherUiEvent
}

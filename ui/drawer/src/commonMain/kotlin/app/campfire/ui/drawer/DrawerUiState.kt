package app.campfire.ui.drawer

import app.campfire.core.model.Server
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class DrawerUiState(
  val eventSink: (DrawerUiEvent) -> Unit,
) : CircuitUiState

sealed interface DrawerUiEvent : CircuitUiEvent {
  data class SwitchAccount(val server: Server) : DrawerUiEvent
  data object AddAccount : DrawerUiEvent
  data class ItemClick(val item: HomeNavigationItem) : DrawerUiEvent
}

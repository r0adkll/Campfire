package app.campfire.ui.drawer

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class DrawerUiState(
  val eventSink: (DrawerUiEvent) -> Unit,
) : CircuitUiState

sealed interface DrawerUiEvent : CircuitUiEvent {
  data class ItemClick(val item: HomeNavigationItem) : DrawerUiEvent
}

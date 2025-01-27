package app.campfire.attribution

import app.campfire.core.coroutines.LoadState
import com.mikepenz.aboutlibraries.Libs
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class AttributionUiState(
  val attributionState: LoadState<out Libs>,
  val eventSink: (AttributionUiEvent) -> Unit,
) : CircuitUiState

sealed interface AttributionUiEvent : CircuitUiEvent {
  data object Back : AttributionUiEvent
}

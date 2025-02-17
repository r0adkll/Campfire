package app.campfire.debug.screen

import app.campfire.core.coroutines.LoadState
import app.campfire.debug.screen.model.EventUiModel
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import kotlinx.collections.immutable.ImmutableList

data class DebugUiState(
  val filter: String,
  val events: LoadState<out ImmutableList<EventUiModel>>,
  val eventSink: (DebugUiEvent) -> Unit,
) : CircuitUiState

sealed interface DebugUiEvent : CircuitUiEvent {
  data object Back : DebugUiEvent
  data class Query(val query: String) : DebugUiEvent
  data object ClearQuery : DebugUiEvent
}

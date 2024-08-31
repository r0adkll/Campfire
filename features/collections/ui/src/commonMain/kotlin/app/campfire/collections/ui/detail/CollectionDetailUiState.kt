package app.campfire.collections.ui.detail

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class CollectionDetailUiState(
  val eventSink: (CollectionDetailUiEvent) -> Unit,
) : CircuitUiState

sealed interface CollectionDetailUiEvent : CircuitUiEvent

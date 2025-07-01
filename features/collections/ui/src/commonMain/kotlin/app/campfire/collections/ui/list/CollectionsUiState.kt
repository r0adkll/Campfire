package app.campfire.collections.ui.list

import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.Collection
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class CollectionsUiState(
  val collectionContentState: LoadState<out List<Collection>>,
  val eventSink: (CollectionsUiEvent) -> Unit,
) : CircuitUiState

sealed interface CollectionsUiEvent : CircuitUiEvent {
  data class CollectionClick(val collection: Collection) : CollectionsUiEvent
}

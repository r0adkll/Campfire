package app.campfire.collections.ui.list

import app.campfire.core.model.Collection
import app.campfire.core.model.CollectionId
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class CollectionsUiState(
  val collectionContentState: CollectionContentState,
  val eventSink: (CollectionsUiEvent) -> Unit,
) : CircuitUiState

sealed interface CollectionContentState {
  data object Loading : CollectionContentState
  data class Loaded(
    val collections: List<Collection>,
  ) : CollectionContentState
  data object Error : CollectionContentState
}

sealed interface CollectionsUiEvent : CircuitUiEvent {
  data class CollectionClick(val collection: Collection) : CollectionsUiEvent
  data class CollectionCreated(
    val id: CollectionId,
    val name: String,
  ) : CollectionsUiEvent
}

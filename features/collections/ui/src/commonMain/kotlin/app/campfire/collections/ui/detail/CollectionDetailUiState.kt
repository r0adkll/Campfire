package app.campfire.collections.ui.detail

import app.campfire.core.model.Collection
import app.campfire.core.model.LibraryItem
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class CollectionDetailUiState(
  val collection: Collection?,
  val collectionContentState: CollectionContentState,
  val eventSink: (CollectionDetailUiEvent) -> Unit,
) : CircuitUiState

sealed interface CollectionContentState {
  data object Loading : CollectionContentState
  data object Error : CollectionContentState
  data class Loaded(val items: List<LibraryItem>) : CollectionContentState
}

sealed interface CollectionDetailUiEvent : CircuitUiEvent {
  data object Back : CollectionDetailUiEvent
  data object Delete : CollectionDetailUiEvent

  data class LibraryItemClick(val libraryItem: LibraryItem) : CollectionDetailUiEvent
  data class DeleteItems(val items: List<LibraryItem>) : CollectionDetailUiEvent
}

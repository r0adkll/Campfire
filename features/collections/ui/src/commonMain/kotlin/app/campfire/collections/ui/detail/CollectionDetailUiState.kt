package app.campfire.collections.ui.detail

import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.Collection
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class CollectionDetailUiState(
  val canEdit: Boolean,
  val collection: Collection?,
  val collectionContentState: LoadState<out List<LibraryItem>>,
  val offlineStates: Map<LibraryItemId, OfflineDownload>,
  val eventSink: (CollectionDetailUiEvent) -> Unit,
) : CircuitUiState

sealed interface CollectionDetailUiEvent : CircuitUiEvent {
  data object Back : CollectionDetailUiEvent
  data object Delete : CollectionDetailUiEvent

  data class LibraryItemClick(val libraryItem: LibraryItem) : CollectionDetailUiEvent
  data class DeleteItems(val items: List<LibraryItem>) : CollectionDetailUiEvent
}

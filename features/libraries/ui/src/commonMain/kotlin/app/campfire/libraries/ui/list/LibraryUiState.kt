package app.campfire.libraries.ui.list

import androidx.compose.runtime.Immutable
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.settings.ItemDisplayState
import app.campfire.core.settings.SortDirection
import app.campfire.core.settings.SortMode
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class LibraryUiState(
  val contentState: LoadState<out List<LibraryItem>>,
  val itemDisplayState: ItemDisplayState,
  val sort: LibrarySort,
  val offlineStates: Map<LibraryItemId, OfflineDownload>,
  val eventSink: (LibraryUiEvent) -> Unit,
) : CircuitUiState

@Immutable
data class LibrarySort(
  val mode: SortMode,
  val direction: SortDirection,
)

sealed interface LibraryUiEvent : CircuitUiEvent {
  data object ToggleItemDisplayState : LibraryUiEvent
  data object FilterClick : LibraryUiEvent
  data class SortModeSelected(val mode: SortMode) : LibraryUiEvent
  data class ItemClick(val libraryItem: LibraryItem) : LibraryUiEvent
}

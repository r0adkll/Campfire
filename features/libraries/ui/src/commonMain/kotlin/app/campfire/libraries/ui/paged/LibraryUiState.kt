package app.campfire.libraries.ui.paged

import androidx.compose.runtime.Immutable
import androidx.paging.Pager
import androidx.paging.PagingData
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.settings.ItemDisplayState
import app.campfire.core.settings.SortDirection
import app.campfire.core.settings.SortMode
import app.campfire.libraries.api.LibraryItemFilter
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import kotlinx.coroutines.flow.Flow

data class LibraryUiState(
  val contentState: LoadState<out Flow<PagingData<LibraryItem>>>,
  val itemDisplayState: ItemDisplayState,
  val totalItemCount: Int,
  val sort: LibrarySort,
  val filter: LibraryItemFilter?,
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
  data class SortModeSelected(val mode: SortMode) : LibraryUiEvent
  data class ItemFilterSelected(val filter: LibraryItemFilter?) : LibraryUiEvent
  data class ItemClick(val libraryItem: LibraryItem) : LibraryUiEvent
}

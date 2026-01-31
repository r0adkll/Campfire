package app.campfire.libraries.ui.paged

import androidx.compose.runtime.Immutable
import androidx.paging.compose.LazyPagingItems
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.core.filter.ContentFilter
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.ItemDisplayState
import app.campfire.core.settings.SortDirection
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class LibraryUiState(
  val lazyPagingItems: LazyPagingItems<LibraryItem>,
  val itemDisplayState: ItemDisplayState,
  val totalItemCount: Int,
  val sort: LibrarySort,
  val filter: ContentFilter?,
  val offlineStates: Map<LibraryItemId, OfflineDownload>,
  val eventSink: (LibraryUiEvent) -> Unit,
) : CircuitUiState

@Immutable
data class LibrarySort(
  val mode: ContentSortMode,
  val direction: SortDirection,
)

sealed interface LibraryUiEvent : CircuitUiEvent {
  data object ToggleItemDisplayState : LibraryUiEvent
  data class SortModeSelected(val mode: ContentSortMode) : LibraryUiEvent
  data class ItemFilterSelected(val filter: ContentFilter?) : LibraryUiEvent
  data class ItemClick(val libraryItem: LibraryItem) : LibraryUiEvent
}

package app.campfire.libraries.ui.list

import androidx.compose.runtime.Immutable
import app.campfire.core.model.LibraryItem
import app.campfire.core.settings.ItemDisplayState
import app.campfire.core.settings.SortDirection
import app.campfire.core.settings.SortMode
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class LibraryUiState(
  val contentState: LibraryContentState,
  val itemDisplayState: ItemDisplayState,
  val sort: LibrarySort,
  val eventSink: (LibraryUiEvent) -> Unit,
) : CircuitUiState

sealed interface LibraryContentState {
  data object Loading : LibraryContentState
  data class Loaded(val items: List<LibraryItem>) : LibraryContentState
  data object Error : LibraryContentState

  fun map(mapper: (List<LibraryItem>) -> List<LibraryItem>): LibraryContentState = when (this) {
    Loading -> this
    Error -> this
    is Loaded -> Loaded(mapper(items))
  }
}

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

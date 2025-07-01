package app.campfire.home.ui

import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.Author
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.Series
import app.campfire.home.api.model.Shelf
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class HomeUiState(
  val homeFeed: LoadState<out List<Shelf<*>>>,
  val offlineStates: Map<LibraryItemId, OfflineDownload>,
  val eventSink: (HomeUiEvent) -> Unit,
) : CircuitUiState

sealed interface HomeUiEvent : CircuitUiEvent {
  data class OpenLibraryItem(val item: LibraryItem) : HomeUiEvent
  data class OpenSeries(val series: Series) : HomeUiEvent
  data class OpenAuthor(val author: Author) : HomeUiEvent
}

package app.campfire.series.ui.detail

import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class SeriesDetailUiState(
  val seriesContentState: LoadState<out List<LibraryItem>>,
  val offlineStates: Map<LibraryItemId, OfflineDownload>,
  val eventSink: (SeriesDetailUiEvent) -> Unit,
) : CircuitUiState

sealed interface SeriesDetailUiEvent : CircuitUiEvent {
  data object Back : SeriesDetailUiEvent
  data class LibraryItemClick(val libraryItem: LibraryItem) : SeriesDetailUiEvent
}

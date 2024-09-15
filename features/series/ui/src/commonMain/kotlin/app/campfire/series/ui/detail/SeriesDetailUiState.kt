package app.campfire.series.ui.detail

import app.campfire.core.model.LibraryItem
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class SeriesDetailUiState(
  val seriesContentState: SeriesContentState,
  val eventSink: (SeriesDetailUiEvent) -> Unit,
) : CircuitUiState

sealed interface SeriesContentState {
  data object Loading : SeriesContentState
  data object Error : SeriesContentState
  data class Loaded(val items: List<LibraryItem>) : SeriesContentState
}

sealed interface SeriesDetailUiEvent : CircuitUiEvent {
  data object Back : SeriesDetailUiEvent
  data class LibraryItemClick(val libraryItem: LibraryItem) : SeriesDetailUiEvent
}

package app.campfire.series.ui.list

import app.campfire.core.model.Series
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class SeriesUiState(
  val seriesContentState: SeriesContentState,
  val eventSink: (SeriesUiEvent) -> Unit,
) : CircuitUiState

sealed interface SeriesContentState {
  data object Loading : SeriesContentState
  data class Loaded(val series: List<Series>) : SeriesContentState
  data object Error : SeriesContentState
}

sealed interface SeriesUiEvent : CircuitUiEvent {
  data class SeriesClicked(val series: Series) : SeriesUiEvent
}

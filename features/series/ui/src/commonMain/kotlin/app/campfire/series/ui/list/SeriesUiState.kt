package app.campfire.series.ui.list

import androidx.compose.runtime.Immutable
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.Series
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Immutable
data class SeriesUiState(
  val seriesContentState: LoadState<out List<Series>>,
  val eventSink: (SeriesUiEvent) -> Unit,
) : CircuitUiState

sealed interface SeriesUiEvent : CircuitUiEvent {
  data class SeriesClicked(val series: Series) : SeriesUiEvent
}

package app.campfire.series.ui.list

import androidx.compose.runtime.Immutable
import androidx.paging.PagingData
import app.campfire.core.coroutines.LoadState
import app.campfire.core.filter.ContentFilter
import app.campfire.core.model.Series
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.SortDirection
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import kotlinx.coroutines.flow.Flow

@Immutable
data class SeriesUiState(
  val totalCount: Int,
  val seriesContentState: LoadState<out Flow<PagingData<Series>>>,
  val filter: ContentFilter?,
  val sortMode: ContentSortMode,
  val sortDirection: SortDirection,
  val eventSink: (SeriesUiEvent) -> Unit,
) : CircuitUiState

sealed interface SeriesUiEvent : CircuitUiEvent {
  data class SeriesClicked(val series: Series) : SeriesUiEvent
  data class FilterChanged(val filter: ContentFilter?) : SeriesUiEvent
  data class SortModeChanged(val mode: ContentSortMode) : SeriesUiEvent
}

package app.campfire.series.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.paging.cachedIn
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.ContentSelected
import app.campfire.analytics.events.ContentType
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.common.screens.SeriesScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.coroutines.map
import app.campfire.core.di.UserScope
import app.campfire.core.filter.ContentFilter
import app.campfire.series.api.SeriesRepository
import app.campfire.series.api.paging.SeriesPager
import app.campfire.settings.api.CampfireSettings
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.retained.rememberRetainedSaveable
import com.slack.circuit.runtime.Navigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

internal const val INVALID_SERIES_COUNT = -1

@CircuitInject(SeriesScreen::class, UserScope::class)
@Inject
class SeriesPresenter(
  @Assisted private val navigator: Navigator,
  private val seriesRepository: SeriesRepository,
  private val settings: CampfireSettings,
  private val analytics: Analytics,
) : NonPausablePresenter<SeriesUiState> {

  @OptIn(ExperimentalCoroutinesApi::class)
  @Composable
  override fun present(): SeriesUiState {
    val scope = rememberCoroutineScope()

    var filter by rememberRetainedSaveable {
      mutableStateOf<ContentFilter?>(null)
    }

    val sortMode by remember {
      settings.observeSeriesSortMode()
    }.collectAsState(settings.seriesSortMode)

    val sortDirection by remember {
      settings.observeSeriesSortDirection()
    }.collectAsState(settings.seriesSortDirection)

    val seriesPagerState by remember(filter, sortMode, sortDirection) {
      seriesRepository.observeSeriesPager(
        filter = filter,
        sortMode = sortMode,
        sortDirection = sortDirection,
      )
        .map { LoadState.Loaded(it) }
        .catch<LoadState<out SeriesPager>> { emit(LoadState.Error) }
    }.collectAsState(LoadState.Loading)

    val seriesContentState by remember {
      derivedStateOf {
        seriesPagerState.map {
          it.pager.flow.cachedIn(scope)
        }
      }
    }

    val totalSeriesCount by remember {
      snapshotFlow { seriesPagerState.dataOrNull }
        .flatMapLatest { pager ->
          pager?.countFlow
            ?.map { it ?: INVALID_SERIES_COUNT }
            ?: flowOf(INVALID_SERIES_COUNT)
        }
    }.collectAsState(INVALID_SERIES_COUNT)

    return SeriesUiState(
      totalCount = totalSeriesCount,
      seriesContentState = seriesContentState,
      filter = filter,
      sortMode = sortMode,
      sortDirection = sortDirection,
    ) { event ->
      when (event) {
        is SeriesUiEvent.SeriesClicked -> {
          analytics.send(ContentSelected(ContentType.Series))
          navigator.goTo(SeriesDetailScreen(event.series.id, event.series.name))
        }

        is SeriesUiEvent.FilterChanged -> {
          analytics.send(ActionEvent("series_item_filter", "selected"))
          filter = event.filter
        }

        is SeriesUiEvent.SortModeChanged -> {
          analytics.send(ActionEvent("series_sort_mode", "selected", event.mode.storageKey))
          if (sortMode == event.mode) {
            settings.seriesSortDirection = sortDirection.flip()
          }
          settings.seriesSortMode = event.mode
        }
      }
    }
  }
}

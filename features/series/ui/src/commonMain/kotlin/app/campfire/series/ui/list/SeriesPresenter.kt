package app.campfire.series.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.ContentSelected
import app.campfire.analytics.events.ContentType
import app.campfire.common.compose.util.rememberRetainedCoroutineScope
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.common.screens.SeriesScreen
import app.campfire.core.coroutines.map
import app.campfire.core.di.UserScope
import app.campfire.core.filter.ContentFilter
import app.campfire.series.api.SeriesRepository
import app.campfire.settings.api.CampfireSettings
import app.campfire.user.api.UserRepository
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.retained.rememberRetainedSaveable
import com.slack.circuit.runtime.Navigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

internal const val INVALID_SERIES_COUNT = -1

@CircuitInject(SeriesScreen::class, UserScope::class)
@Inject
class SeriesPresenter(
  @Assisted private val navigator: Navigator,
  private val userRepository: UserRepository,
  private val seriesRepository: SeriesRepository,
  private val settings: CampfireSettings,
  private val analytics: Analytics,
) : NonPausablePresenter<SeriesUiState> {

  @OptIn(ExperimentalCoroutinesApi::class)
  @Composable
  override fun present(): SeriesUiState {
    // Using a pager requires us to remember the coroutine scope passed the
    // composition of this pager / ui. We should remember it until this screen
    // leaves the back stack
    val scope = rememberRetainedCoroutineScope()

    var filter by rememberRetainedSaveable {
      mutableStateOf<ContentFilter?>(null)
    }

    val sortMode by remember {
      settings.observeSeriesSortMode()
    }.collectAsState(settings.seriesSortMode)

    val sortDirection by remember {
      settings.observeSeriesSortDirection()
    }.collectAsState(settings.seriesSortDirection)

    val currentUser by remember {
      userRepository.observeStatefulCurrentUser()
    }.collectAsState()

    val lazyPagingItems = rememberRetained(currentUser, filter, sortMode, sortDirection) {
      seriesRepository.createSeriesPager(
        user = currentUser,
        filter = filter,
        sortMode = sortMode,
        sortDirection = sortDirection,
      ).flow.cachedIn(scope)
    }.collectAsLazyPagingItems()

    val totalSeriesCount by remember {
      seriesRepository.observeFilteredSeriesCount(
        filter = filter,
        sortMode = sortMode,
        sortDirection = sortDirection,
      ).map { it ?: INVALID_SERIES_COUNT }
    }.collectAsState(INVALID_SERIES_COUNT)

    return SeriesUiState(
      totalCount = totalSeriesCount,
      lazyPagingItems = lazyPagingItems,
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

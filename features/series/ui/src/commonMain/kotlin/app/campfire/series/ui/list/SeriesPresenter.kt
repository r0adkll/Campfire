// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

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
import app.campfire.settings.api.LibraryViewSettings
import app.campfire.user.api.UserRepository
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.retained.rememberRetainedSaveable
import com.slack.circuit.runtime.Navigator
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal const val INVALID_SERIES_COUNT = -1

@CircuitInject(SeriesScreen::class, UserScope::class)
@Inject
class SeriesPresenter(
  private val navigator: Navigator,
  private val userRepository: UserRepository,
  private val seriesRepository: SeriesRepository,
  private val libraryViewSettings: LibraryViewSettings,
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
      libraryViewSettings.observeSeriesSortMode()
    }.collectAsState(null)

    val sortDirection by remember {
      libraryViewSettings.observeSeriesSortDirection()
    }.collectAsState(null)

    val displayState by remember {
      libraryViewSettings.observeSeriesDisplayState()
    }.collectAsState(null)

    val currentUser by userRepository.userFlow.collectAsState()

    val lazyPagingItems = rememberRetained(
      currentUser.id,
      currentUser.selectedLibraryId,
      filter,
      sortMode,
      sortDirection,
    ) {
      val mode = sortMode
      val direction = sortDirection
      // Nothing is listed until the stored sort is known
      if (mode == null || direction == null) {
        emptyFlow()
      } else {
        seriesRepository.createSeriesPager(
          user = currentUser,
          filter = filter,
          sortMode = mode,
          sortDirection = direction,
        ).flow.cachedIn(scope)
      }
    }.collectAsLazyPagingItems()

    val totalSeriesCount by remember(filter, sortMode, sortDirection) {
      val mode = sortMode
      val direction = sortDirection
      if (mode == null || direction == null) {
        flowOf(INVALID_SERIES_COUNT)
      } else {
        seriesRepository.observeFilteredSeriesCount(
          filter = filter,
          sortMode = mode,
          sortDirection = direction,
        ).map { it ?: INVALID_SERIES_COUNT }
      }
    }.collectAsState(INVALID_SERIES_COUNT)

    return SeriesUiState(
      totalCount = totalSeriesCount,
      lazyPagingItems = lazyPagingItems,
      filter = filter,
      sortMode = sortMode,
      sortDirection = sortDirection,
      displayState = displayState,
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
            sortDirection?.flip()?.let(libraryViewSettings::setSeriesSortDirection)
          }
          libraryViewSettings.setSeriesSortMode(event.mode)
        }

        SeriesUiEvent.ToggleDisplayState -> {
          analytics.send(ActionEvent("series_display_state", "toggle"))
          displayState?.next()?.let(libraryViewSettings::setSeriesDisplayState)
        }
      }
    }
  }
}

// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.common.screens.UrlScreen
import app.campfire.core.di.UserScope
import app.campfire.discover.api.DiscoverScanTracker
import app.campfire.discover.api.screen.DiscoverScreen
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(DiscoverScreen::class, UserScope::class)
@Inject
class DiscoverPresenter(
  @Assisted private val navigator: Navigator,
  private val tracker: DiscoverScanTracker,
) : NonPausablePresenter<DiscoverUiState> {

  @Composable
  override fun present(): DiscoverUiState {
    val scanState by tracker.state.collectAsState()
    var selectedTab by rememberRetained { mutableStateOf(DiscoverTab.Missing) }

    return DiscoverUiState(
      scanState = scanState,
      selectedTab = selectedTab,
    ) { event ->
      when (event) {
        DiscoverUiEvent.Back -> navigator.pop()
        DiscoverUiEvent.Scan -> tracker.startScan()
        DiscoverUiEvent.CancelScan -> tracker.cancelScan()
        is DiscoverUiEvent.SelectTab -> selectedTab = event.tab
        is DiscoverUiEvent.SeriesClick -> navigator.goTo(
          SeriesDetailScreen(seriesId = event.seriesId, seriesName = event.seriesName),
        )
        is DiscoverUiEvent.BookClick -> navigator.goTo(UrlScreen(event.url))
      }
    }
  }
}

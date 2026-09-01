// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover.ui

import androidx.compose.runtime.Immutable
import app.campfire.core.model.SeriesId
import app.campfire.discover.api.DiscoverScanState
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Immutable
data class DiscoverUiState(
  val scanState: DiscoverScanState,
  val selectedTab: DiscoverTab,
  val eventSink: (DiscoverUiEvent) -> Unit,
) : CircuitUiState

enum class DiscoverTab {
  Missing,
  Upcoming,
}

sealed interface DiscoverUiEvent : CircuitUiEvent {
  data object Back : DiscoverUiEvent
  data object Refresh : DiscoverUiEvent
  data object CancelScan : DiscoverUiEvent
  data class SelectTab(val tab: DiscoverTab) : DiscoverUiEvent
  data class SeriesClick(val seriesId: SeriesId, val seriesName: String) : DiscoverUiEvent
  data class BookClick(val url: String) : DiscoverUiEvent
}

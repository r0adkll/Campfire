// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover.ui

import androidx.compose.runtime.Immutable
import app.campfire.discover.api.DiscoverScanState
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Immutable
data class UpcomingUiState(
  val scanState: DiscoverScanState,
  val eventSink: (UpcomingUiEvent) -> Unit,
) : CircuitUiState

sealed interface UpcomingUiEvent : CircuitUiEvent {
  data object Back : UpcomingUiEvent
  data object Refresh : UpcomingUiEvent
  data object CancelScan : UpcomingUiEvent
  data class BookClick(val url: String) : UpcomingUiEvent
}

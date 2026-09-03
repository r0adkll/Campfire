// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover.ui

import androidx.compose.runtime.Immutable
import app.campfire.bookinfo.api.UpcomingRelease
import app.campfire.discover.api.DiscoverScanState
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class UpcomingUiState(
  val scanState: DiscoverScanState,
  /**
   * The upcoming releases from the registry's series cache — live during a
   * scan, and already populated on open from previous scans.
   */
  val upcoming: ImmutableList<UpcomingRelease>,
  val eventSink: (UpcomingUiEvent) -> Unit,
) : CircuitUiState

sealed interface UpcomingUiEvent : CircuitUiEvent {
  data object Back : UpcomingUiEvent
  data object Refresh : UpcomingUiEvent
  data object CancelScan : UpcomingUiEvent
  data class BookClick(val url: String) : UpcomingUiEvent
}

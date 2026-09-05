// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.navigation.rail

import androidx.compose.runtime.Stable
import app.campfire.ui.navigation.HomeNavigationItem
import com.slack.circuit.runtime.CircuitUiState

@Stable
data class WideNavigationRailUiState(
  val navigationItems: List<HomeNavigationItem>,
  val expanded: Boolean,
  val eventSink: (WideNavigationRailUiEvent) -> Unit,
) : CircuitUiState

@Stable
sealed interface WideNavigationRailUiEvent {
  data object ToggleExpanded : WideNavigationRailUiEvent
}

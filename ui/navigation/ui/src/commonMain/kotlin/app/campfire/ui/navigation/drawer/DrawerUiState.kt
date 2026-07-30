// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.navigation.drawer

import androidx.compose.runtime.Stable
import app.campfire.settings.api.ThemeMode
import app.campfire.ui.navigation.HomeNavigationItem
import com.slack.circuit.runtime.CircuitUiState

@Stable
data class DrawerUiState(
  val themeMode: ThemeMode,
  val navigationItems: List<HomeNavigationItem>,
  val eventSink: (DrawerUiEvent) -> Unit,
) : CircuitUiState

@Stable
sealed interface DrawerUiEvent {
  data object CycleThemeMode : DrawerUiEvent
}

// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.navigation.drawer

import androidx.compose.runtime.Stable
import app.campfire.network.reachability.OfflineChoice
import app.campfire.network.reachability.OfflineState
import app.campfire.settings.api.ThemeMode
import app.campfire.ui.navigation.HomeNavigationItem
import com.slack.circuit.runtime.CircuitUiState

@Stable
data class DrawerUiState(
  val themeMode: ThemeMode,
  val offline: OfflineState,
  /** Whether the Wi-Fi based way to work offline applies on this device. */
  val canDetectWifi: Boolean,
  val navigationItems: List<HomeNavigationItem>,
  val eventSink: (DrawerUiEvent) -> Unit,
) : CircuitUiState

@Stable
sealed interface DrawerUiEvent {
  data object CycleThemeMode : DrawerUiEvent
  data class GoOffline(val choice: OfflineChoice) : DrawerUiEvent
  data object GoOnline : DrawerUiEvent
}

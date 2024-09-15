package app.campfire.ui.settings

import app.campfire.common.settings.CampfireSettings
import app.campfire.core.app.ApplicationInfo
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class SettingsUiState(
  val theme: CampfireSettings.Theme,
  val useDynamicColors: Boolean,
  val applicationInfo: ApplicationInfo,
  val eventSink: (SettingsUiEvent) -> Unit,
) : CircuitUiState

sealed interface SettingsUiEvent : CircuitUiEvent {
  data object Back : SettingsUiEvent
  data class Theme(val theme: CampfireSettings.Theme) : SettingsUiEvent
  data class UseDynamicColors(val useDynamicColors: Boolean) : SettingsUiEvent
}

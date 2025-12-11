package app.campfire.ui.theming.ui.picker

import app.campfire.core.coroutines.LoadState
import app.campfire.ui.theming.api.AppTheme
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class ThemePickerUiState(
  val currentTheme: AppTheme,
  val builtInThemes: List<AppTheme>,
  val customThemes: LoadState<out List<AppTheme.Fixed.Custom>>,
  val eventSink: (ThemePickerUiEvent) -> Unit,
) : CircuitUiState

sealed interface ThemePickerUiEvent : CircuitUiEvent {
  data object Back : ThemePickerUiEvent
  data class OpenThemeBuilder(
    val theme: AppTheme.Fixed.Custom? = null,
  ) : ThemePickerUiEvent

  data class SelectTheme(val theme: AppTheme) : ThemePickerUiEvent
}

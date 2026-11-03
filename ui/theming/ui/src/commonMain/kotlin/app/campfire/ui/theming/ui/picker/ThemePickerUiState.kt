// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.picker

import app.campfire.core.coroutines.LoadState
import app.campfire.settings.api.ThemeMode
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.ui.preview.PreviewContent
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class ThemePickerUiState(
  val currentTheme: AppTheme,
  /** Light, dark or following the system: the same setting as Appearance and the drawer. */
  val themeMode: ThemeMode,
  val builtInThemes: List<AppTheme>,
  val customThemes: LoadState<out List<AppTheme.Fixed>>,
  val aiThemeBuilderAvailable: Boolean,
  /** What each theme's preview shows: the user's own books once they load. */
  val previewContent: PreviewContent,
  val eventSink: (ThemePickerUiEvent) -> Unit,
) : CircuitUiState

sealed interface ThemePickerUiEvent : CircuitUiEvent {
  data object Back : ThemePickerUiEvent
  data class OpenThemeBuilder(
    val theme: AppTheme.Fixed? = null,
  ) : ThemePickerUiEvent

  data object OpenAiThemeBuilder : ThemePickerUiEvent

  data class SelectTheme(val theme: AppTheme) : ThemePickerUiEvent

  data class SetThemeMode(val mode: ThemeMode) : ThemePickerUiEvent
}

/** Identifies a theme across reloads, where a custom theme's palette may compare unequal. */
internal val AppTheme.key: String
  get() = when (this) {
    is AppTheme.Fixed -> id
    AppTheme.Dynamic -> "Dynamic"
  }

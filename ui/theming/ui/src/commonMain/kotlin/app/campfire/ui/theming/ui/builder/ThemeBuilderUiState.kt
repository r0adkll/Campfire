// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.ui.preview.PreviewContent
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpec
import com.r0adkll.swatchbuckler.color.dynamiccolor.Variant
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Immutable
data class ThemeBuilderUiState(
  val theme: AppTheme.Fixed.Custom,
  val name: TextFieldState,
  val styles: List<StyleOption>,
  val palettes: List<PaletteRow>,
  val contrast: List<ContrastStatus>,
  /** How many custom themes already exist, for suggesting a name. */
  val customThemeCount: Int,
  /** The user's books and cover theming settings, for the preview. */
  val previewContent: PreviewContent,
  val eventSink: (ThemeBuilderUiEvent) -> Unit,
) : CircuitUiState {

  val isNew: Boolean get() = theme.isNew
  val seedColor: Color get() = theme.seedColor
  val colorStyle: Variant get() = theme.colorStyle
  val colorSpec: ColorSpec.SpecVersion get() = theme.colorSpec
  val contrastLevel: ContrastLevel get() = ContrastLevel.from(theme.contrastLevel)

  fun failingChecks(dark: Boolean) = contrast.filterNot { it.passes(dark) }.map { it.check }.toSet()
}

sealed interface ThemeBuilderUiEvent : CircuitUiEvent {
  data object Back : ThemeBuilderUiEvent
  data object Save : ThemeBuilderUiEvent
  data object Delete : ThemeBuilderUiEvent

  data class IconPicked(val icon: AppTheme.Icon) : ThemeBuilderUiEvent

  /** Picking the [PaletteKey.Accent] color changes the seed and keeps every other pick. */
  data class ColorPicked(val key: PaletteKey, val color: Color) : ThemeBuilderUiEvent
  data class ColorReset(val key: PaletteKey) : ThemeBuilderUiEvent

  /** The color spec follows the style. */
  data class StyleClick(val style: Variant) : ThemeBuilderUiEvent
  data class ContrastLevelClick(val level: ContrastLevel) : ThemeBuilderUiEvent

  /** Opens the settings where book details and the player stop using cover colors. */
  data object CoverThemingSettings : ThemeBuilderUiEvent
}

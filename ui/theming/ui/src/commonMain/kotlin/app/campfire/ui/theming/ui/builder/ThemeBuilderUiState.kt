// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import app.campfire.common.compose.theme.ColorPalette
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.RoleOverrides
import app.campfire.ui.theming.api.ThemeRole
import app.campfire.ui.theming.ui.builder.color.ContrastCheck
import app.campfire.ui.theming.ui.preview.PreviewContent
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpec
import com.r0adkll.swatchbuckler.color.dynamiccolor.Variant
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Immutable
data class ThemeBuilderUiState(
  /** The theme as it will be saved, fine-tunes included. The preview draws this. */
  val theme: AppTheme.Fixed.Custom,
  /**
   * The palette before fine-tuning. The builder's own controls draw with it, so a bad fine-tune
   * can never make the builder itself hard to use.
   */
  val generatedPalette: ColorPalette,
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
  val roleOverrides: RoleOverrides get() = theme.roleOverrides

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

  /** Sets one role to an exact color in one mode. */
  data class RoleColorPicked(val role: ThemeRole, val dark: Boolean, val color: Color) : ThemeBuilderUiEvent

  /** Puts the generated colors back for [roles] in one mode. */
  data class RolesReset(val roles: Set<ThemeRole>, val dark: Boolean) : ThemeBuilderUiEvent

  /** Moves [check]'s foreground lightness until it's readable in one mode. */
  data class FixContrast(val check: ContrastCheck, val dark: Boolean) : ThemeBuilderUiEvent

  /**
   * Copies the fine-tuned [roles] to the other mode with their lightness flipped, then fixes
   * [check] there if the flip made it hard to read.
   */
  data class MirrorRoles(
    val roles: Set<ThemeRole>,
    val fromDark: Boolean,
    val check: ContrastCheck?,
  ) : ThemeBuilderUiEvent
}

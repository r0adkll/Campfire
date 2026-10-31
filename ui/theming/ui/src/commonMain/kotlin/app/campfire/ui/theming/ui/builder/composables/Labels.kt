// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.campfire.ui.theming.ui.builder.ContrastLevel
import app.campfire.ui.theming.ui.builder.PaletteKey
import app.campfire.ui.theming.ui.builder.color.ContrastCheck
import campfire.ui.theming.ui.generated.resources.Res
import campfire.ui.theming.ui.generated.resources.contrast_check_account_card
import campfire.ui.theming.ui.generated.resources.contrast_check_borders
import campfire.ui.theming.ui.generated.resources.contrast_check_buttons
import campfire.ui.theming.ui.generated.resources.contrast_check_card_text
import campfire.ui.theming.ui.generated.resources.contrast_check_dialog_buttons
import campfire.ui.theming.ui.generated.resources.contrast_check_drawer_items
import campfire.ui.theming.ui.generated.resources.contrast_check_error_text
import campfire.ui.theming.ui.generated.resources.contrast_check_headers
import campfire.ui.theming.ui.generated.resources.contrast_check_navigation_bar
import campfire.ui.theming.ui.generated.resources.contrast_check_offline_status
import campfire.ui.theming.ui.generated.resources.contrast_check_progress_bars
import campfire.ui.theming.ui.generated.resources.contrast_check_screen_text
import campfire.ui.theming.ui.generated.resources.contrast_check_selected_items
import campfire.ui.theming.ui.generated.resources.contrast_check_selected_tab_name
import campfire.ui.theming.ui.generated.resources.contrast_check_snackbars
import campfire.ui.theming.ui.generated.resources.contrast_check_supporting_text
import campfire.ui.theming.ui.generated.resources.contrast_check_warnings
import campfire.ui.theming.ui.generated.resources.theme_contrast_high
import campfire.ui.theming.ui.generated.resources.theme_contrast_medium
import campfire.ui.theming.ui.generated.resources.theme_contrast_standard
import campfire.ui.theming.ui.generated.resources.theme_palette_accent
import campfire.ui.theming.ui.generated.resources.theme_palette_backgrounds
import campfire.ui.theming.ui.generated.resources.theme_palette_errors
import campfire.ui.theming.ui.generated.resources.theme_palette_supporting
import campfire.ui.theming.ui.generated.resources.theme_palette_third_accent
import campfire.ui.theming.ui.generated.resources.theme_style_content
import campfire.ui.theming.ui.generated.resources.theme_style_content_description
import campfire.ui.theming.ui.generated.resources.theme_style_expressive
import campfire.ui.theming.ui.generated.resources.theme_style_expressive_description
import campfire.ui.theming.ui.generated.resources.theme_style_fidelity
import campfire.ui.theming.ui.generated.resources.theme_style_fidelity_description
import campfire.ui.theming.ui.generated.resources.theme_style_fruit_salad
import campfire.ui.theming.ui.generated.resources.theme_style_fruit_salad_description
import campfire.ui.theming.ui.generated.resources.theme_style_monochrome
import campfire.ui.theming.ui.generated.resources.theme_style_monochrome_description
import campfire.ui.theming.ui.generated.resources.theme_style_neutral
import campfire.ui.theming.ui.generated.resources.theme_style_neutral_description
import campfire.ui.theming.ui.generated.resources.theme_style_rainbow
import campfire.ui.theming.ui.generated.resources.theme_style_rainbow_description
import campfire.ui.theming.ui.generated.resources.theme_style_tonal
import campfire.ui.theming.ui.generated.resources.theme_style_tonal_description
import campfire.ui.theming.ui.generated.resources.theme_style_vibrant
import campfire.ui.theming.ui.generated.resources.theme_style_vibrant_description
import com.r0adkll.swatchbuckler.color.dynamiccolor.Variant
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun Variant.label(): String = stringResource(
  when (this) {
    Variant.EXPRESSIVE -> Res.string.theme_style_expressive
    Variant.VIBRANT -> Res.string.theme_style_vibrant
    Variant.TONAL_SPOT -> Res.string.theme_style_tonal
    Variant.NEUTRAL -> Res.string.theme_style_neutral
    Variant.FIDELITY -> Res.string.theme_style_fidelity
    Variant.CONTENT -> Res.string.theme_style_content
    Variant.MONOCHROME -> Res.string.theme_style_monochrome
    Variant.RAINBOW -> Res.string.theme_style_rainbow
    Variant.FRUIT_SALAD -> Res.string.theme_style_fruit_salad
  },
)

@Composable
internal fun Variant.description(): String = stringResource(
  when (this) {
    Variant.EXPRESSIVE -> Res.string.theme_style_expressive_description
    Variant.VIBRANT -> Res.string.theme_style_vibrant_description
    Variant.TONAL_SPOT -> Res.string.theme_style_tonal_description
    Variant.NEUTRAL -> Res.string.theme_style_neutral_description
    Variant.FIDELITY -> Res.string.theme_style_fidelity_description
    Variant.CONTENT -> Res.string.theme_style_content_description
    Variant.MONOCHROME -> Res.string.theme_style_monochrome_description
    Variant.RAINBOW -> Res.string.theme_style_rainbow_description
    Variant.FRUIT_SALAD -> Res.string.theme_style_fruit_salad_description
  },
)

@Composable
internal fun ContrastCheck.label(): String = stringResource(
  when (this) {
    ContrastCheck.ScreenText -> Res.string.contrast_check_screen_text
    ContrastCheck.CardText -> Res.string.contrast_check_card_text
    ContrastCheck.SupportingText -> Res.string.contrast_check_supporting_text
    ContrastCheck.DialogButtons -> Res.string.contrast_check_dialog_buttons
    ContrastCheck.Headers -> Res.string.contrast_check_headers
    ContrastCheck.Buttons -> Res.string.contrast_check_buttons
    ContrastCheck.ProgressBars -> Res.string.contrast_check_progress_bars
    ContrastCheck.AccountCard -> Res.string.contrast_check_account_card
    ContrastCheck.SelectedItems -> Res.string.contrast_check_selected_items
    ContrastCheck.SelectedTabName -> Res.string.contrast_check_selected_tab_name
    ContrastCheck.NavigationBar -> Res.string.contrast_check_navigation_bar
    ContrastCheck.DrawerItems -> Res.string.contrast_check_drawer_items
    ContrastCheck.OfflineStatus -> Res.string.contrast_check_offline_status
    ContrastCheck.ErrorText -> Res.string.contrast_check_error_text
    ContrastCheck.Warnings -> Res.string.contrast_check_warnings
    ContrastCheck.Borders -> Res.string.contrast_check_borders
    ContrastCheck.Snackbars -> Res.string.contrast_check_snackbars
  },
)

@Composable
internal fun PaletteKey.label(): String = stringResource(
  when (this) {
    PaletteKey.Accent -> Res.string.theme_palette_accent
    PaletteKey.Supporting -> Res.string.theme_palette_supporting
    PaletteKey.ThirdAccent -> Res.string.theme_palette_third_accent
    PaletteKey.Backgrounds -> Res.string.theme_palette_backgrounds
    PaletteKey.Errors -> Res.string.theme_palette_errors
  },
)

@Composable
internal fun ContrastLevel.label(): String = stringResource(
  when (this) {
    ContrastLevel.Normal -> Res.string.theme_contrast_standard
    ContrastLevel.Medium -> Res.string.theme_contrast_medium
    ContrastLevel.High -> Res.string.theme_contrast_high
  },
)

/** `#C0532D`: the color as six uppercase hex digits behind a hash. */
internal fun Color.hexCode(): String = "#" + (toArgb() and 0xFFFFFF).toString(16).padStart(6, '0').uppercase()

/** A contrast ratio to one decimal place, such as `4.5`. */
internal fun Double.ratioText(): String = ((this * 10).roundToInt() / 10.0).toString()

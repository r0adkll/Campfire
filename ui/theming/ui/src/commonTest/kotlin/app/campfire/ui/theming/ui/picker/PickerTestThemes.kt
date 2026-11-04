// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.picker

import androidx.compose.ui.graphics.Color
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.HalogenStyle
import app.campfire.ui.theming.ui.builder.color.CustomThemeColors
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpec.SpecVersion
import com.r0adkll.swatchbuckler.color.dynamiccolor.Variant

/** Every built-in theme, as Android lists them. */
internal val BuiltInThemes = listOf(
  AppTheme.Fixed.Tent,
  AppTheme.Fixed.Forest,
  AppTheme.Fixed.WaterBottle,
  AppTheme.Fixed.Rucksack,
  AppTheme.Fixed.LifeFloat,
  AppTheme.Fixed.Mountain,
  AppTheme.Dynamic,
)

internal fun customTheme(
  id: String,
  name: String,
  seed: Color = Color(0xFF2E7D6B),
): AppTheme.Fixed.Custom = AppTheme.Fixed.Custom(
  id = id,
  name = name,
  seedColor = seed,
  colorSpec = SpecVersion.SPEC_2025,
  colorStyle = Variant.TONAL_SPOT,
  contrastLevel = 0f,
  secondaryColorOverride = null,
  tertiaryColorOverride = null,
  errorColorOverride = null,
  neutralColorOverride = null,
  neutralVariantColorOverride = null,
  icon = AppTheme.Icon.Campfire,
  colorPalette = CustomThemeColors(seed, Variant.TONAL_SPOT).toColorPalette(),
)

internal fun aiTheme(): AppTheme.Fixed.Ai = AppTheme.Fixed.Ai(
  id = "dusk",
  name = "Dusk over the lake",
  prompt = "dusk over the lake",
  style = HalogenStyle.entries.first(),
  icon = AppTheme.Icon.Night,
  colorPalette = CustomThemeColors(Color(0xFF5B5BC2), Variant.VIBRANT).toColorPalette(),
)

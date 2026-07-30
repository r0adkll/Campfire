// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.theme

import androidx.compose.material3.ColorScheme

data class ColorPalette(
  val lightColorScheme: ColorScheme,
  val darkColorScheme: ColorScheme,
  val mediumContrastLightColorScheme: ColorScheme? = null,
  val highContrastLightColorScheme: ColorScheme? = null,
  val mediumContrastDarkColorScheme: ColorScheme? = null,
  val highContrastDarkColorScheme: ColorScheme? = null,
)

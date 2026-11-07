// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun colorScheme(
  colorPalette: ColorPalette,
  useDarkColors: Boolean,
  useDynamicColors: Boolean,
): ColorScheme = when {
  useDynamicColors -> rememberDynamicColorScheme(useDarkColors)
  useDarkColors -> colorPalette.darkColorScheme
  else -> colorPalette.lightColorScheme
}

@Composable
internal actual fun dynamicColorScheme(
  useDarkColors: Boolean,
): ColorScheme? = rememberDynamicColorScheme(useDarkColors)

/**
 * The wallpaper's color scheme, built once rather than on every recomposition: a new instance
 * re-provides Material's static color local and redraws everything beneath the theme.
 */
@Composable
private fun rememberDynamicColorScheme(useDarkColors: Boolean): ColorScheme {
  val context = LocalContext.current
  // Wallpaper color changes arrive as a configuration change
  val configuration = LocalConfiguration.current
  return remember(context, configuration, useDarkColors) {
    if (useDarkColors) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
  }
}

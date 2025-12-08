// Copyright 2023, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: Apache-2.0

package app.campfire.common.compose.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun colorScheme(
  colorPalette: ColorPalette,
  useDarkColors: Boolean,
  useDynamicColors: Boolean,
): ColorScheme = when {
  useDynamicColors && useDarkColors -> {
    dynamicDarkColorScheme(LocalContext.current)
  }
  useDynamicColors && !useDarkColors -> {
    dynamicLightColorScheme(LocalContext.current)
  }
  useDarkColors -> colorPalette.darkColorScheme
  else -> colorPalette.lightColorScheme
}

@Composable
internal actual fun dynamicColorScheme(
  useDarkColors: Boolean,
): ColorScheme? = when {
  useDarkColors -> dynamicDarkColorScheme(LocalContext.current)
  else -> dynamicLightColorScheme(LocalContext.current)
}

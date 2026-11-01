// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import app.campfire.common.compose.theme.color.CampfireColorScheme
import app.campfire.common.compose.theme.color.LocalCampfireColorScheme
import app.campfire.common.compose.theme.color.campfireColorScheme
import app.campfire.common.compose.theme.color.harmonizedWith
import app.campfire.common.compose.theme.tents.RedColorPalette
import com.r0adkll.swatchbuckler.compose.Theme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CampfireTheme(
  colorPalette: ColorPalette = RedColorPalette,
  useDarkColors: Boolean = isSystemInDarkTheme(),
  useDynamicColors: Boolean = false,
  content: @Composable () -> Unit,
) {
  ApplyStatusBar(useDarkColors)

  CompositionLocalProvider(LocalUseDarkColors provides useDarkColors) {
    CampfireMaterialTheme(
      colorScheme = colorScheme(colorPalette, useDarkColors, useDynamicColors),
      useDarkColors = useDarkColors,
      content = content,
    )
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CampfireTheme(
  colorScheme: @Composable () -> ColorScheme,
  useDarkColors: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  ApplyStatusBar(useDarkColors)

  // The color scheme may read LocalUseDarkColors, so it's built inside the provider.
  CompositionLocalProvider(LocalUseDarkColors provides useDarkColors) {
    CampfireMaterialTheme(
      colorScheme = colorScheme(),
      useDarkColors = useDarkColors,
      content = content,
    )
  }
}

/**
 * Material's theme with Campfire's type and shapes, plus success and loading colors harmonized
 * with [colorScheme]'s primary color.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CampfireMaterialTheme(
  colorScheme: ColorScheme,
  useDarkColors: Boolean,
  content: @Composable () -> Unit,
) {
  val campfireColorScheme = remember(useDarkColors, colorScheme.primary) {
    campfireColorScheme(useDarkColors).harmonizedWith(colorScheme.primary)
  }

  CompositionLocalProvider(LocalCampfireColorScheme provides campfireColorScheme) {
    MaterialExpressiveTheme(
      colorScheme = colorScheme,
      typography = CampfireTypography,
      shapes = CampfireShapes,
      content = content,
    )
  }
}

object CampfireTheme {

  val colorScheme: CampfireColorScheme
    @Composable get() = LocalCampfireColorScheme.current
}

val LocalUseDarkColors = compositionLocalOf { false }

val Theme.colorScheme: ColorScheme
  @Composable get() = if (LocalUseDarkColors.current) darkColorScheme else lightColorScheme

@Composable
expect fun ApplyStatusBar(useDarkColors: Boolean)

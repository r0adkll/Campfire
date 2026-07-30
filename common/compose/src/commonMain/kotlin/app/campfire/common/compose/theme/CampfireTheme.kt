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
import app.campfire.common.compose.theme.color.CampfireColorScheme
import app.campfire.common.compose.theme.color.LocalCampfireColorScheme
import app.campfire.common.compose.theme.color.darkCampfireColorScheme
import app.campfire.common.compose.theme.color.lightCampfireColorScheme
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

  val campfireColorScheme = if (useDarkColors) darkCampfireColorScheme() else lightCampfireColorScheme()

  CompositionLocalProvider(
    LocalUseDarkColors provides useDarkColors,
    LocalCampfireColorScheme provides campfireColorScheme,
  ) {
    val colorScheme = colorScheme(colorPalette, useDarkColors, useDynamicColors)

    MaterialExpressiveTheme(
      colorScheme = colorScheme,
      typography = CampfireTypography,
      shapes = CampfireShapes,
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

  val campfireColorScheme = if (useDarkColors) darkCampfireColorScheme() else lightCampfireColorScheme()

  CompositionLocalProvider(
    LocalUseDarkColors provides useDarkColors,
    LocalCampfireColorScheme provides campfireColorScheme,
  ) {
    val colorScheme = colorScheme()

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

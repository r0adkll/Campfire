package app.campfire.common.compose.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
expect fun colorScheme(
  colorPalette: ColorPalette,
  useDarkColors: Boolean,
  useDynamicColors: Boolean,
): ColorScheme

@Composable
internal expect fun dynamicColorScheme(
  useDarkColors: Boolean,
): ColorScheme?

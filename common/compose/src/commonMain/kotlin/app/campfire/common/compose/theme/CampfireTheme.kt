package app.campfire.common.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import app.campfire.core.model.Tent

@Composable
fun CampfireTheme(
  tent: Tent = Tent.Default,
  useDarkColors: Boolean = isSystemInDarkTheme(),
  useDynamicColors: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorPalette = tent.colorPalette
  val colorScheme = colorScheme(colorPalette, useDarkColors, useDynamicColors)
  ApplyStatusBar(useDarkColors, colorScheme)
  MaterialTheme(
    colorScheme = colorScheme,
    typography = CampfireTypography,
    shapes = CampfireShapes,
    content = content,
  )
}

@Composable
expect fun ApplyStatusBar(useDarkColors: Boolean, colorScheme: ColorScheme)

package app.campfire.common.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun CampfireTheme(
  useDarkColors: Boolean = isSystemInDarkTheme(),
  useDynamicColors: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = colorScheme(useDarkColors, useDynamicColors)
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

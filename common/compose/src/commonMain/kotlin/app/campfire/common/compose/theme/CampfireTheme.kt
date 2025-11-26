package app.campfire.common.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import app.campfire.core.model.Tent
import com.r0adkll.swatchbuckler.compose.Theme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CampfireTheme(
  tent: Tent = Tent.Default,
  useDarkColors: Boolean = isSystemInDarkTheme(),
  useDynamicColors: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorPalette = tent.colorPalette
  val colorScheme = colorScheme(colorPalette, useDarkColors, useDynamicColors)
  ApplyStatusBar(useDarkColors)

  CompositionLocalProvider(
    LocalUseDarkColors provides useDarkColors,
  ) {
    MaterialExpressiveTheme(
      colorScheme = colorScheme,
      typography = CampfireTypography,
      shapes = CampfireShapes,
      content = content,
    )
  }
}

val LocalUseDarkColors = compositionLocalOf { false }

val Theme.colorScheme: ColorScheme
  @Composable get() = if (LocalUseDarkColors.current) darkColorScheme else lightColorScheme

@Composable
expect fun ApplyStatusBar(useDarkColors: Boolean)

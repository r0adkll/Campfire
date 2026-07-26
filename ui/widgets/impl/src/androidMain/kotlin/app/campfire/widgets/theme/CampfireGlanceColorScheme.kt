package app.campfire.widgets.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.glance.color.ColorProviders
import androidx.glance.material3.ColorProviders
import androidx.glance.unit.ColorProvider
import app.campfire.common.compose.theme.tents.RedColorPalette
import com.r0adkll.swatchbuckler.compose.Theme

object CampfireGlanceColorScheme {

  val palette = RedColorPalette

  val colors = ColorProviders(
    light = palette.lightColorScheme,
    dark = palette.darkColorScheme,
  )
}

fun Theme.asColorProviders(): ColorProviders = ColorProviders(
  light = lightColorScheme,
  dark = darkColorScheme,
)

val LocalContentColorProvider = compositionLocalOf<ColorProvider> {
  error("No local color provider in this composition")
}

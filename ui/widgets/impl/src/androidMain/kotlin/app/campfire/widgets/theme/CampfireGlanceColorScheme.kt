package app.campfire.widgets.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.glance.color.ColorProviders
import androidx.glance.material3.ColorProviders
import androidx.glance.unit.ColorProvider
import app.campfire.common.compose.theme.colorPalette
import app.campfire.core.model.Tent
import com.r0adkll.swatchbuckler.compose.Theme

object CampfireGlanceColorScheme {

  val palette = Tent.Default.colorPalette

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

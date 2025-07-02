package app.campfire.widgets.theme

import androidx.glance.material3.ColorProviders
import app.campfire.common.compose.theme.colorPalette
import app.campfire.core.model.Tent

object CampfireGlanceColorScheme {

  val palette = Tent.Default.colorPalette

  val colors = ColorProviders(
    light = palette.lightColorScheme,
    dark = palette.darkColorScheme,
  )
}

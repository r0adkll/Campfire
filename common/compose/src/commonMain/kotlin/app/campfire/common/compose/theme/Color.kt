package app.campfire.common.compose.theme

import androidx.compose.material3.ColorScheme
import app.campfire.common.compose.theme.alt.AltBlueColorPalette
import app.campfire.common.compose.theme.alt.AltGreenColorPalette
import app.campfire.common.compose.theme.alt.AltYellowColorPalette
import app.campfire.common.compose.theme.tents.OrangeColorPalette
import app.campfire.common.compose.theme.tents.PurpleColorPalette
import app.campfire.common.compose.theme.tents.RedColorPalette
import app.campfire.core.model.Tent

data class ColorPalette(
  val lightColorScheme: ColorScheme,
  val darkColorScheme: ColorScheme,
  val mediumContrastLightColorScheme: ColorScheme? = null,
  val highContrastLightColorScheme: ColorScheme? = null,
  val mediumContrastDarkColorScheme: ColorScheme? = null,
  val highContrastDarkColorScheme: ColorScheme? = null,
)

/**
 * Get the [ColorPalette] for a given tent
 */
val Tent.colorPalette: ColorPalette
  get() = when (this) {
    Tent.Red -> RedColorPalette
    Tent.Blue -> AltBlueColorPalette
    Tent.Green -> AltGreenColorPalette
    Tent.Yellow -> AltYellowColorPalette
    Tent.Orange -> OrangeColorPalette
    Tent.Purple -> PurpleColorPalette
  }

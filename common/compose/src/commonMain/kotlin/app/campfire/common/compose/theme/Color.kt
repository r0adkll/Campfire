package app.campfire.common.compose.theme

import androidx.compose.material3.ColorScheme
import app.campfire.common.compose.theme.tents.BlueColorPalette
import app.campfire.common.compose.theme.tents.GreenColorPalette
import app.campfire.common.compose.theme.tents.OrangeColorPalette
import app.campfire.common.compose.theme.tents.PurpleColorPalette
import app.campfire.common.compose.theme.tents.RedColorPalette
import app.campfire.common.compose.theme.tents.YellowColorPalette
import app.campfire.core.model.Tent

data class ColorPalette(
  val lightColorScheme: ColorScheme,
  val darkColorScheme: ColorScheme,
  val mediumContrastLightColorScheme: ColorScheme,
  val highContrastLightColorScheme: ColorScheme,
  val mediumContrastDarkColorScheme: ColorScheme,
  val highContrastDarkColorScheme: ColorScheme,
)

/**
 * Get the [ColorPalette] for a given tent
 */
val Tent.colorPalette: ColorPalette
  get() = when (this) {
    Tent.Red -> RedColorPalette
    Tent.Blue -> BlueColorPalette
    Tent.Green -> GreenColorPalette
    Tent.Yellow -> YellowColorPalette
    Tent.Orange -> OrangeColorPalette
    Tent.Purple -> PurpleColorPalette
  }

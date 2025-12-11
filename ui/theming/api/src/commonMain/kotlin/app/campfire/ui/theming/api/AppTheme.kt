package app.campfire.ui.theming.api

import androidx.compose.foundation.Image
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.theme.Forest
import app.campfire.common.compose.icons.theme.LifeFloat
import app.campfire.common.compose.icons.theme.Mountain
import app.campfire.common.compose.icons.theme.Rucksack
import app.campfire.common.compose.icons.theme.Tent
import app.campfire.common.compose.icons.theme.WaterBottle
import app.campfire.common.compose.icons.theme.rememberWallVectorPainter
import app.campfire.common.compose.theme.ColorPalette
import app.campfire.common.compose.theme.LocalUseDarkColors
import app.campfire.common.compose.theme.alt.AltBlueColorPalette
import app.campfire.common.compose.theme.alt.AltGreenColorPalette
import app.campfire.common.compose.theme.alt.AltOrangeColorPalette
import app.campfire.common.compose.theme.alt.AltPurpleColorPalette
import app.campfire.common.compose.theme.alt.AltYellowColorPalette
import app.campfire.common.compose.theme.colorScheme
import app.campfire.common.compose.theme.tents.RedColorPalette
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpec
import com.r0adkll.swatchbuckler.color.dynamiccolor.Variant

sealed interface AppTheme {

  sealed class Fixed(
    open val icon: Icon,
    open val colorPalette: ColorPalette,
  ) : AppTheme {
    data object Tent : Fixed(Icon.Tent, RedColorPalette)
    data object Rucksack : Fixed(Icon.Rucksack, AltYellowColorPalette)
    data object WaterBottle : Fixed(Icon.WaterBottle, AltBlueColorPalette)
    data object Forest : Fixed(Icon.Forest, AltGreenColorPalette)
    data object Mountain : Fixed(Icon.Mountain, AltPurpleColorPalette)
    data object LifeFloat : Fixed(Icon.LifeFloat, AltOrangeColorPalette)

    data class Custom(
      val id: String,
      val name: String,
      val seedColor: Color,
      val colorSpec: ColorSpec.SpecVersion,
      val colorStyle: Variant,
      val contrastLevel: Float,
      val secondaryColorOverride: Color?,
      val tertiaryColorOverride: Color?,
      val errorColorOverride: Color?,
      val neutralColorOverride: Color?,
      val neutralVariantColorOverride: Color?,
      override val icon: Icon,
      override val colorPalette: ColorPalette,
    ) : Fixed(icon, colorPalette)
  }

  data object Dynamic : AppTheme

  enum class Icon(val icon: @Composable () -> ImageVector) {
    Tent(icon = { CampfireIcons.Theme.Tent }),
    Rucksack(icon = { CampfireIcons.Theme.Rucksack }),
    WaterBottle(icon = { CampfireIcons.Theme.WaterBottle }),
    Forest(icon = { CampfireIcons.Theme.Forest }),
    Mountain(icon = { CampfireIcons.Theme.Mountain }),
    LifeFloat(icon = { CampfireIcons.Theme.LifeFloat }),
  }
}

@Composable
fun colorScheme(
  theme: AppTheme,
  useDarkColors: Boolean = LocalUseDarkColors.current,
): ColorScheme = when (theme) {
  is AppTheme.Fixed -> {
    val palette = theme.colorPalette
    colorScheme(palette, useDarkColors, false)
  }
  is AppTheme.Dynamic -> {
    val backupPalette = AppTheme.Fixed.Tent.colorPalette
    colorScheme(backupPalette, useDarkColors, true)
  }
}

@Composable
fun AppThemeImage(
  appTheme: AppTheme,
  modifier: Modifier,
) {
  when (appTheme) {
    is AppTheme.Fixed -> {
      Image(
        appTheme.icon.icon(),
        contentDescription = null,
        modifier = modifier,
      )
    }

    is AppTheme.Dynamic -> {
      Image(
        rememberWallVectorPainter(),
        contentDescription = null,
        modifier = modifier,
      )
    }
  }
}

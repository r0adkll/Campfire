// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.api

import androidx.compose.foundation.Image
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.theme.Airplane
import app.campfire.common.compose.icons.theme.Axe
import app.campfire.common.compose.icons.theme.BeachUmbrella
import app.campfire.common.compose.icons.theme.Bicycle
import app.campfire.common.compose.icons.theme.Binoculars
import app.campfire.common.compose.icons.theme.Cactus
import app.campfire.common.compose.icons.theme.Camera
import app.campfire.common.compose.icons.theme.Camper
import app.campfire.common.compose.icons.theme.Campfire
import app.campfire.common.compose.icons.theme.Cloud
import app.campfire.common.compose.icons.theme.Compass
import app.campfire.common.compose.icons.theme.CrescentMoon
import app.campfire.common.compose.icons.theme.Deer
import app.campfire.common.compose.icons.theme.Dinghy
import app.campfire.common.compose.icons.theme.Evergreen
import app.campfire.common.compose.icons.theme.Fishing
import app.campfire.common.compose.icons.theme.Flashlight
import app.campfire.common.compose.icons.theme.Forest
import app.campfire.common.compose.icons.theme.Globe
import app.campfire.common.compose.icons.theme.HotAirBalloon
import app.campfire.common.compose.icons.theme.Lake
import app.campfire.common.compose.icons.theme.Lantern
import app.campfire.common.compose.icons.theme.LifeFloat
import app.campfire.common.compose.icons.theme.LogCabin
import app.campfire.common.compose.icons.theme.MapleLeaf
import app.campfire.common.compose.icons.theme.Mountain
import app.campfire.common.compose.icons.theme.Mushroom
import app.campfire.common.compose.icons.theme.Night
import app.campfire.common.compose.icons.theme.Owl
import app.campfire.common.compose.icons.theme.PaperMap
import app.campfire.common.compose.icons.theme.ParkBench
import app.campfire.common.compose.icons.theme.Rucksack
import app.campfire.common.compose.icons.theme.SailBoat
import app.campfire.common.compose.icons.theme.SleepingBag
import app.campfire.common.compose.icons.theme.Snowflake
import app.campfire.common.compose.icons.theme.Star
import app.campfire.common.compose.icons.theme.Suitcase
import app.campfire.common.compose.icons.theme.Sun
import app.campfire.common.compose.icons.theme.Tent
import app.campfire.common.compose.icons.theme.Train
import app.campfire.common.compose.icons.theme.Trekking
import app.campfire.common.compose.icons.theme.WaterBottle
import app.campfire.common.compose.icons.theme.Waterfall
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
    open val id: String,
    open val icon: Icon,
    open val colorPalette: ColorPalette,
  ) : AppTheme {
    data object Tent : Fixed("Tent", Icon.Tent, RedColorPalette)
    data object Rucksack : Fixed("Rucksack", Icon.Rucksack, AltYellowColorPalette)
    data object WaterBottle : Fixed("WaterBottle", Icon.WaterBottle, AltBlueColorPalette)
    data object Forest : Fixed("Forest", Icon.Forest, AltGreenColorPalette)
    data object Mountain : Fixed("Mountain", Icon.Mountain, AltPurpleColorPalette)
    data object LifeFloat : Fixed("LifeFloat", Icon.LifeFloat, AltOrangeColorPalette)

    data class Custom(
      override val id: String,
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
    ) : Fixed(id, icon, colorPalette)

    data class Ai(
      override val id: String,
      val name: String,
      val prompt: String,
      val style: HalogenStyle,
      override val icon: Icon,
      override val colorPalette: ColorPalette,
    ) : Fixed(id, icon, colorPalette)
  }

  data object Dynamic : AppTheme

  enum class Icon(val icon: @Composable () -> ImageVector) {
    Tent(icon = { CampfireIcons.Theme.Tent }),
    Rucksack(icon = { CampfireIcons.Theme.Rucksack }),
    WaterBottle(icon = { CampfireIcons.Theme.WaterBottle }),
    Forest(icon = { CampfireIcons.Theme.Forest }),
    Mountain(icon = { CampfireIcons.Theme.Mountain }),
    LifeFloat(icon = { CampfireIcons.Theme.LifeFloat }),
    Campfire(icon = { CampfireIcons.Theme.Campfire }),
    Compass(icon = { CampfireIcons.Theme.Compass }),
    Binoculars(icon = { CampfireIcons.Theme.Binoculars }),
    Lantern(icon = { CampfireIcons.Theme.Lantern }),
    PaperMap(icon = { CampfireIcons.Theme.PaperMap }),
    Axe(icon = { CampfireIcons.Theme.Axe }),
    SleepingBag(icon = { CampfireIcons.Theme.SleepingBag }),
    Evergreen(icon = { CampfireIcons.Theme.Evergreen }),
    Suitcase(icon = { CampfireIcons.Theme.Suitcase }),
    LogCabin(icon = { CampfireIcons.Theme.LogCabin }),
    Dinghy(icon = { CampfireIcons.Theme.Dinghy }),
    Fishing(icon = { CampfireIcons.Theme.Fishing }),
    HotAirBalloon(icon = { CampfireIcons.Theme.HotAirBalloon }),
    Airplane(icon = { CampfireIcons.Theme.Airplane }),
    Camera(icon = { CampfireIcons.Theme.Camera }),
    Flashlight(icon = { CampfireIcons.Theme.Flashlight }),
    Waterfall(icon = { CampfireIcons.Theme.Waterfall }),
    Camper(icon = { CampfireIcons.Theme.Camper }),
    Trekking(icon = { CampfireIcons.Theme.Trekking }),
    Sun(icon = { CampfireIcons.Theme.Sun }),
    CrescentMoon(icon = { CampfireIcons.Theme.CrescentMoon }),
    SailBoat(icon = { CampfireIcons.Theme.SailBoat }),
    Globe(icon = { CampfireIcons.Theme.Globe }),
    MapleLeaf(icon = { CampfireIcons.Theme.MapleLeaf }),
    Snowflake(icon = { CampfireIcons.Theme.Snowflake }),
    Lake(icon = { CampfireIcons.Theme.Lake }),
    ParkBench(icon = { CampfireIcons.Theme.ParkBench }),
    Cactus(icon = { CampfireIcons.Theme.Cactus }),
    Mushroom(icon = { CampfireIcons.Theme.Mushroom }),
    Cloud(icon = { CampfireIcons.Theme.Cloud }),
    Star(icon = { CampfireIcons.Theme.Star }),
    Bicycle(icon = { CampfireIcons.Theme.Bicycle }),
    Train(icon = { CampfireIcons.Theme.Train }),
    BeachUmbrella(icon = { CampfireIcons.Theme.BeachUmbrella }),
    Deer(icon = { CampfireIcons.Theme.Deer }),
    Owl(icon = { CampfireIcons.Theme.Owl }),
    Night(icon = { CampfireIcons.Theme.Night }),
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

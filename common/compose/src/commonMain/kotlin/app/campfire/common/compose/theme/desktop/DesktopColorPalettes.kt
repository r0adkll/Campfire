// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.theme.desktop

import androidx.compose.ui.graphics.Color
import app.campfire.common.compose.theme.ColorPalette
import app.campfire.common.compose.theme.alt.AltBlueColorPalette
import app.campfire.common.compose.theme.alt.AltGreenColorPalette
import app.campfire.common.compose.theme.alt.AltOrangeColorPalette
import app.campfire.common.compose.theme.alt.AltPurpleColorPalette
import app.campfire.common.compose.theme.alt.AltYellowColorPalette
import app.campfire.common.compose.theme.tents.RedColorPalette

/**
 * Accent seeds for the desktop palettes. Each is the light primary of the palette it
 * mirrors, and only its hue survives: the vibrant accent takes the maximum chroma that hue
 * can hold. Swap any of these for a hand-picked hex to retune a single theme.
 */
private val TentAccent: Color get() = RedColorPalette.lightColorScheme.primary
private val RucksackAccent: Color get() = AltYellowColorPalette.lightColorScheme.primary
private val WaterBottleAccent: Color get() = AltBlueColorPalette.lightColorScheme.primary
private val ForestAccent: Color get() = AltGreenColorPalette.lightColorScheme.primary
private val MountainAccent: Color get() = AltPurpleColorPalette.lightColorScheme.primary
private val LifeFloatAccent: Color get() = AltOrangeColorPalette.lightColorScheme.primary

/** Desktop counterparts of the built-in palettes. */
val RedDesktopColorPalette: ColorPalette by lazy { desktopColorPalette(TentAccent) }
val AltYellowDesktopColorPalette: ColorPalette by lazy { desktopColorPalette(RucksackAccent) }
val AltBlueDesktopColorPalette: ColorPalette by lazy { desktopColorPalette(WaterBottleAccent) }
val AltGreenDesktopColorPalette: ColorPalette by lazy { desktopColorPalette(ForestAccent) }
val AltPurpleDesktopColorPalette: ColorPalette by lazy { desktopColorPalette(MountainAccent) }
val AltOrangeDesktopColorPalette: ColorPalette by lazy { desktopColorPalette(LifeFloatAccent) }

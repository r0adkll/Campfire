// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.DeveloperMode: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "DeveloperMode",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(280f, 840f)
      verticalLineToRelative(-720f)
      verticalLineToRelative(720f)
      close()
      moveTo(508.5f, 228.5f)
      quadTo(520f, 217f, 520f, 200f)
      reflectiveQuadToRelative(-11.5f, -28.5f)
      quadTo(497f, 160f, 480f, 160f)
      reflectiveQuadToRelative(-28.5f, 11.5f)
      quadTo(440f, 183f, 440f, 200f)
      reflectiveQuadToRelative(11.5f, 28.5f)
      quadTo(463f, 240f, 480f, 240f)
      reflectiveQuadToRelative(28.5f, -11.5f)
      close()
      moveTo(280f, 920f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(200f, 840f)
      verticalLineToRelative(-720f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(280f, 40f)
      horizontalLineToRelative(400f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(760f, 120f)
      verticalLineToRelative(124f)
      quadToRelative(18f, 7f, 29f, 22f)
      reflectiveQuadToRelative(11f, 34f)
      verticalLineToRelative(80f)
      quadToRelative(0f, 19f, -11f, 34f)
      reflectiveQuadToRelative(-29f, 22f)
      verticalLineToRelative(44f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(720f, 520f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(680f, 480f)
      verticalLineToRelative(-360f)
      lineTo(280f, 120f)
      verticalLineToRelative(720f)
      horizontalLineToRelative(40f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(360f, 880f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(320f, 920f)
      horizontalLineToRelative(-40f)
      close()
      moveTo(553f, 760f)
      lineTo(612f, 819f)
      quadToRelative(11f, 11f, 11f, 27.5f)
      reflectiveQuadTo(612f, 875f)
      quadToRelative(-12f, 12f, -28.5f, 12f)
      reflectiveQuadTo(555f, 875f)
      lineToRelative(-87f, -87f)
      quadToRelative(-12f, -12f, -12f, -28f)
      reflectiveQuadToRelative(12f, -28f)
      lineToRelative(88f, -88f)
      quadToRelative(12f, -12f, 28f, -11.5f)
      reflectiveQuadToRelative(28f, 12.5f)
      quadToRelative(11f, 12f, 11.5f, 28f)
      reflectiveQuadTo(612f, 701f)
      lineToRelative(-59f, 59f)
      close()
      moveTo(807f, 760f)
      lineTo(748f, 701f)
      quadToRelative(-11f, -11f, -11f, -27.5f)
      reflectiveQuadToRelative(11f, -28.5f)
      quadToRelative(12f, -12f, 28.5f, -12f)
      reflectiveQuadToRelative(28.5f, 12f)
      lineToRelative(87f, 87f)
      quadToRelative(12f, 12f, 12f, 28f)
      reflectiveQuadToRelative(-12f, 28f)
      lineToRelative(-88f, 88f)
      quadToRelative(-12f, 12f, -28f, 11.5f)
      reflectiveQuadTo(748f, 875f)
      quadToRelative(-11f, -12f, -11.5f, -28f)
      reflectiveQuadToRelative(11.5f, -28f)
      lineToRelative(59f, -59f)
      close()
    }
  }.build()
}

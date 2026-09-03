// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.Logout: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "Logout",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
    autoMirror = true,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(200f, 840f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(120f, 760f)
      verticalLineToRelative(-560f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(200f, 120f)
      horizontalLineToRelative(240f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(480f, 160f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(440f, 200f)
      lineTo(200f, 200f)
      verticalLineToRelative(560f)
      horizontalLineToRelative(240f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(480f, 800f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(440f, 840f)
      lineTo(200f, 840f)
      close()
      moveTo(687f, 520f)
      lineTo(400f, 520f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(360f, 480f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(400f, 440f)
      horizontalLineToRelative(287f)
      lineToRelative(-75f, -75f)
      quadToRelative(-11f, -11f, -11f, -27f)
      reflectiveQuadToRelative(11f, -28f)
      quadToRelative(11f, -12f, 28f, -12.5f)
      reflectiveQuadToRelative(29f, 11.5f)
      lineToRelative(143f, 143f)
      quadToRelative(12f, 12f, 12f, 28f)
      reflectiveQuadToRelative(-12f, 28f)
      lineTo(669f, 651f)
      quadToRelative(-12f, 12f, -28.5f, 11.5f)
      reflectiveQuadTo(612f, 650f)
      quadToRelative(-11f, -12f, -10.5f, -28.5f)
      reflectiveQuadTo(613f, 594f)
      lineToRelative(74f, -74f)
      close()
    }
  }.build()
}

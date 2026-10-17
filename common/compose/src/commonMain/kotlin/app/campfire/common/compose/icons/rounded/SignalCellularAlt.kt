// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.SignalCellularAlt: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "SignalCellularAlt",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(217.5f, 782.5f)
      quadTo(200f, 765f, 200f, 740f)
      verticalLineToRelative(-120f)
      quadToRelative(0f, -25f, 17.5f, -42.5f)
      reflectiveQuadTo(260f, 560f)
      quadToRelative(25f, 0f, 42.5f, 17.5f)
      reflectiveQuadTo(320f, 620f)
      verticalLineToRelative(120f)
      quadToRelative(0f, 25f, -17.5f, 42.5f)
      reflectiveQuadTo(260f, 800f)
      quadToRelative(-25f, 0f, -42.5f, -17.5f)
      close()
      moveTo(457.5f, 782.5f)
      quadTo(440f, 765f, 440f, 740f)
      verticalLineToRelative(-320f)
      quadToRelative(0f, -25f, 17.5f, -42.5f)
      reflectiveQuadTo(500f, 360f)
      quadToRelative(25f, 0f, 42.5f, 17.5f)
      reflectiveQuadTo(560f, 420f)
      verticalLineToRelative(320f)
      quadToRelative(0f, 25f, -17.5f, 42.5f)
      reflectiveQuadTo(500f, 800f)
      quadToRelative(-25f, 0f, -42.5f, -17.5f)
      close()
      moveTo(697.5f, 782.5f)
      quadTo(680f, 765f, 680f, 740f)
      verticalLineToRelative(-520f)
      quadToRelative(0f, -25f, 17.5f, -42.5f)
      reflectiveQuadTo(740f, 160f)
      quadToRelative(25f, 0f, 42.5f, 17.5f)
      reflectiveQuadTo(800f, 220f)
      verticalLineToRelative(520f)
      quadToRelative(0f, 25f, -17.5f, 42.5f)
      reflectiveQuadTo(740f, 800f)
      quadToRelative(-25f, 0f, -42.5f, -17.5f)
      close()
    }
  }.build()
}

// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.FormatSize: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "FormatSize",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(560f, 280f)
      lineTo(420f, 280f)
      quadToRelative(-25f, 0f, -42.5f, -17.5f)
      reflectiveQuadTo(360f, 220f)
      quadToRelative(0f, -25f, 17.5f, -42.5f)
      reflectiveQuadTo(420f, 160f)
      horizontalLineToRelative(400f)
      quadToRelative(25f, 0f, 42.5f, 17.5f)
      reflectiveQuadTo(880f, 220f)
      quadToRelative(0f, 25f, -17.5f, 42.5f)
      reflectiveQuadTo(820f, 280f)
      lineTo(680f, 280f)
      verticalLineToRelative(460f)
      quadToRelative(0f, 25f, -17.5f, 42.5f)
      reflectiveQuadTo(620f, 800f)
      quadToRelative(-25f, 0f, -42.5f, -17.5f)
      reflectiveQuadTo(560f, 740f)
      verticalLineToRelative(-460f)
      close()
      moveTo(200f, 480f)
      horizontalLineToRelative(-60f)
      quadToRelative(-25f, 0f, -42.5f, -17.5f)
      reflectiveQuadTo(80f, 420f)
      quadToRelative(0f, -25f, 17.5f, -42.5f)
      reflectiveQuadTo(140f, 360f)
      horizontalLineToRelative(240f)
      quadToRelative(25f, 0f, 42.5f, 17.5f)
      reflectiveQuadTo(440f, 420f)
      quadToRelative(0f, 25f, -17.5f, 42.5f)
      reflectiveQuadTo(380f, 480f)
      horizontalLineToRelative(-60f)
      verticalLineToRelative(260f)
      quadToRelative(0f, 25f, -17.5f, 42.5f)
      reflectiveQuadTo(260f, 800f)
      quadToRelative(-25f, 0f, -42.5f, -17.5f)
      reflectiveQuadTo(200f, 740f)
      verticalLineToRelative(-260f)
      close()
    }
  }.build()
}

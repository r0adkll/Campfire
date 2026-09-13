// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.VolumeDown: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "VolumeDown",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
    autoMirror = true,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(360f, 600f)
      lineTo(240f, 600f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(200f, 560f)
      verticalLineToRelative(-160f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(240f, 360f)
      horizontalLineToRelative(120f)
      lineToRelative(132f, -132f)
      quadToRelative(19f, -19f, 43.5f, -8.5f)
      reflectiveQuadTo(560f, 257f)
      verticalLineToRelative(446f)
      quadToRelative(0f, 27f, -24.5f, 37.5f)
      reflectiveQuadTo(492f, 732f)
      lineTo(360f, 600f)
      close()
      moveTo(740f, 480f)
      quadToRelative(0f, 42f, -19f, 79.5f)
      reflectiveQuadTo(671f, 621f)
      quadToRelative(-10f, 6f, -20.5f, 0.5f)
      reflectiveQuadTo(640f, 604f)
      verticalLineToRelative(-250f)
      quadToRelative(0f, -12f, 10.5f, -17.5f)
      reflectiveQuadToRelative(20.5f, 0.5f)
      quadToRelative(31f, 25f, 50f, 63f)
      reflectiveQuadToRelative(19f, 80f)
      close()
      moveTo(480f, 354f)
      lineToRelative(-86f, 86f)
      lineTo(280f, 440f)
      verticalLineToRelative(80f)
      horizontalLineToRelative(114f)
      lineToRelative(86f, 86f)
      verticalLineToRelative(-252f)
      close()
      moveTo(380f, 480f)
      close()
    }
  }.build()
}

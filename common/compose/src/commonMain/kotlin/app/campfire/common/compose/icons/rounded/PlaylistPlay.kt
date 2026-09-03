// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.PlaylistPlay: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "PlaylistPlay",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
    autoMirror = true,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(160f, 640f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(120f, 600f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(160f, 560f)
      horizontalLineToRelative(240f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(440f, 600f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(400f, 640f)
      lineTo(160f, 640f)
      close()
      moveTo(160f, 480f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(120f, 440f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(160f, 400f)
      horizontalLineToRelative(400f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(600f, 440f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(560f, 480f)
      lineTo(160f, 480f)
      close()
      moveTo(160f, 320f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(120f, 280f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(160f, 240f)
      horizontalLineToRelative(400f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(600f, 280f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(560f, 320f)
      lineTo(160f, 320f)
      close()
      moveTo(671f, 819f)
      quadToRelative(-5f, 3f, -10f, 3f)
      reflectiveQuadToRelative(-10f, -2f)
      quadToRelative(-5f, -2f, -8f, -6.5f)
      reflectiveQuadToRelative(-3f, -10.5f)
      verticalLineToRelative(-246f)
      quadToRelative(0f, -6f, 3f, -10.5f)
      reflectiveQuadToRelative(8f, -6.5f)
      quadToRelative(5f, -2f, 10f, -2f)
      reflectiveQuadToRelative(10f, 3f)
      lineToRelative(184f, 122f)
      quadToRelative(5f, 3f, 7f, 7.5f)
      reflectiveQuadToRelative(2f, 9.5f)
      quadToRelative(0f, 5f, -2f, 9.5f)
      reflectiveQuadToRelative(-7f, 7.5f)
      lineTo(671f, 819f)
      close()
    }
  }.build()
}

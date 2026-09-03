// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.QueueMusic: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "QueueMusic",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
    autoMirror = true,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(640f, 800f)
      quadToRelative(-50f, 0f, -85f, -35f)
      reflectiveQuadToRelative(-35f, -85f)
      quadToRelative(0f, -50f, 35f, -85f)
      reflectiveQuadToRelative(85f, -35f)
      quadToRelative(11f, 0f, 21f, 1.5f)
      reflectiveQuadToRelative(19f, 6.5f)
      verticalLineToRelative(-288f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(720f, 240f)
      horizontalLineToRelative(120f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(880f, 280f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(840f, 320f)
      horizontalLineToRelative(-80f)
      verticalLineToRelative(360f)
      quadToRelative(0f, 50f, -35f, 85f)
      reflectiveQuadToRelative(-85f, 35f)
      close()
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
    }
  }.build()
}

// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.PlaylistAddCheck: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "PlaylistAddCheck",
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
      moveTo(639f, 740.5f)
      quadToRelative(-7f, -2.5f, -13f, -8.5f)
      lineToRelative(-86f, -86f)
      quadToRelative(-11f, -11f, -11.5f, -27.5f)
      reflectiveQuadTo(540f, 590f)
      quadToRelative(11f, -11f, 27.5f, -11.5f)
      reflectiveQuadTo(596f, 589f)
      lineToRelative(58f, 57f)
      lineToRelative(141f, -141f)
      quadToRelative(12f, -12f, 28.5f, -11.5f)
      reflectiveQuadTo(852f, 506f)
      quadToRelative(11f, 12f, 11.5f, 28f)
      reflectiveQuadTo(852f, 562f)
      lineTo(682f, 732f)
      quadToRelative(-6f, 6f, -13f, 8.5f)
      reflectiveQuadToRelative(-15f, 2.5f)
      quadToRelative(-8f, 0f, -15f, -2.5f)
      close()
    }
  }.build()
}

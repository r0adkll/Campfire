// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.SdStorage: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "SdStorage",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(428.5f, 428.5f)
      quadTo(440f, 417f, 440f, 400f)
      verticalLineToRelative(-80f)
      quadToRelative(0f, -17f, -11.5f, -28.5f)
      reflectiveQuadTo(400f, 280f)
      quadToRelative(-17f, 0f, -28.5f, 11.5f)
      reflectiveQuadTo(360f, 320f)
      verticalLineToRelative(80f)
      quadToRelative(0f, 17f, 11.5f, 28.5f)
      reflectiveQuadTo(400f, 440f)
      quadToRelative(17f, 0f, 28.5f, -11.5f)
      close()
      moveTo(548.5f, 428.5f)
      quadTo(560f, 417f, 560f, 400f)
      verticalLineToRelative(-80f)
      quadToRelative(0f, -17f, -11.5f, -28.5f)
      reflectiveQuadTo(520f, 280f)
      quadToRelative(-17f, 0f, -28.5f, 11.5f)
      reflectiveQuadTo(480f, 320f)
      verticalLineToRelative(80f)
      quadToRelative(0f, 17f, 11.5f, 28.5f)
      reflectiveQuadTo(520f, 440f)
      quadToRelative(17f, 0f, 28.5f, -11.5f)
      close()
      moveTo(668.5f, 428.5f)
      quadTo(680f, 417f, 680f, 400f)
      verticalLineToRelative(-80f)
      quadToRelative(0f, -17f, -11.5f, -28.5f)
      reflectiveQuadTo(640f, 280f)
      quadToRelative(-17f, 0f, -28.5f, 11.5f)
      reflectiveQuadTo(600f, 320f)
      verticalLineToRelative(80f)
      quadToRelative(0f, 17f, 11.5f, 28.5f)
      reflectiveQuadTo(640f, 440f)
      quadToRelative(17f, 0f, 28.5f, -11.5f)
      close()
      moveTo(240f, 880f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(160f, 800f)
      verticalLineToRelative(-447f)
      quadToRelative(0f, -16f, 6f, -30.5f)
      reflectiveQuadToRelative(17f, -25.5f)
      lineToRelative(194f, -194f)
      quadToRelative(11f, -11f, 25.5f, -17f)
      reflectiveQuadToRelative(30.5f, -6f)
      horizontalLineToRelative(287f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(800f, 160f)
      verticalLineToRelative(640f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(720f, 880f)
      lineTo(240f, 880f)
      close()
      moveTo(240f, 800f)
      horizontalLineToRelative(480f)
      verticalLineToRelative(-640f)
      lineTo(434f, 160f)
      lineTo(240f, 354f)
      verticalLineToRelative(446f)
      close()
      moveTo(240f, 800f)
      horizontalLineToRelative(480f)
      horizontalLineToRelative(-480f)
      close()
    }
  }.build()
}

// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.Brightness6: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "Brightness6",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(346f, 800f)
      lineTo(240f, 800f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(160f, 720f)
      verticalLineToRelative(-106f)
      lineToRelative(-77f, -78f)
      quadToRelative(-11f, -12f, -17f, -26.5f)
      reflectiveQuadTo(60f, 480f)
      quadToRelative(0f, -15f, 6f, -29.5f)
      reflectiveQuadTo(83f, 424f)
      lineToRelative(77f, -78f)
      verticalLineToRelative(-106f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(240f, 160f)
      horizontalLineToRelative(106f)
      lineToRelative(78f, -77f)
      quadToRelative(12f, -11f, 26.5f, -17f)
      reflectiveQuadToRelative(29.5f, -6f)
      quadToRelative(15f, 0f, 29.5f, 6f)
      reflectiveQuadToRelative(26.5f, 17f)
      lineToRelative(78f, 77f)
      horizontalLineToRelative(106f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(800f, 240f)
      verticalLineToRelative(106f)
      lineToRelative(77f, 78f)
      quadToRelative(11f, 12f, 17f, 26.5f)
      reflectiveQuadToRelative(6f, 29.5f)
      quadToRelative(0f, 15f, -6f, 29.5f)
      reflectiveQuadTo(877f, 536f)
      lineToRelative(-77f, 78f)
      verticalLineToRelative(106f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(720f, 800f)
      lineTo(614f, 800f)
      lineToRelative(-78f, 77f)
      quadToRelative(-12f, 11f, -26.5f, 17f)
      reflectiveQuadTo(480f, 900f)
      quadToRelative(-15f, 0f, -29.5f, -6f)
      reflectiveQuadTo(424f, 877f)
      lineToRelative(-78f, -77f)
      close()
      moveTo(380f, 720f)
      lineTo(480f, 820f)
      lineTo(580f, 720f)
      horizontalLineToRelative(140f)
      verticalLineToRelative(-140f)
      lineToRelative(100f, -100f)
      lineToRelative(-100f, -100f)
      verticalLineToRelative(-140f)
      lineTo(580f, 240f)
      lineTo(480f, 140f)
      lineTo(380f, 240f)
      lineTo(240f, 240f)
      verticalLineToRelative(140f)
      lineTo(140f, 480f)
      lineToRelative(100f, 100f)
      verticalLineToRelative(140f)
      horizontalLineToRelative(140f)
      close()
      moveTo(480f, 680f)
      quadToRelative(83f, 0f, 141.5f, -58.5f)
      reflectiveQuadTo(680f, 480f)
      quadToRelative(0f, -83f, -58.5f, -141.5f)
      reflectiveQuadTo(480f, 280f)
      verticalLineToRelative(400f)
      close()
    }
  }.build()
}

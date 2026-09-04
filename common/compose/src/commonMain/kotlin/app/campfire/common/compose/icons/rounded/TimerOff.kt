// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.TimerOff: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "TimerOff",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(480f, 160f)
      quadToRelative(60f, 0f, 117.5f, 20f)
      reflectiveQuadTo(706f, 238f)
      lineToRelative(28f, -28f)
      quadToRelative(11f, -11f, 28f, -11f)
      reflectiveQuadToRelative(28f, 11f)
      quadToRelative(11f, 11f, 11f, 28f)
      reflectiveQuadToRelative(-11f, 28f)
      lineToRelative(-28f, 28f)
      quadToRelative(38f, 51f, 58f, 108.5f)
      reflectiveQuadTo(840f, 520f)
      quadToRelative(0f, 26f, -4f, 51.5f)
      reflectiveQuadTo(824f, 623f)
      quadToRelative(-7f, 22f, -22.5f, 29f)
      reflectiveQuadToRelative(-29.5f, 2f)
      quadToRelative(-14f, -5f, -23.5f, -18f)
      reflectiveQuadToRelative(-3.5f, -30f)
      quadToRelative(8f, -22f, 11.5f, -43.5f)
      reflectiveQuadTo(760f, 520f)
      quadToRelative(0f, -116f, -82f, -198f)
      reflectiveQuadToRelative(-198f, -82f)
      quadToRelative(-20f, 0f, -43f, 3.5f)
      reflectiveQuadTo(391f, 255f)
      quadToRelative(-17f, 5f, -29f, -4f)
      reflectiveQuadToRelative(-17f, -23f)
      quadToRelative(-5f, -14f, 1.5f, -29.5f)
      reflectiveQuadTo(373f, 176f)
      quadToRelative(26f, -8f, 53f, -12f)
      reflectiveQuadToRelative(54f, -4f)
      close()
      moveTo(520f, 408f)
      verticalLineToRelative(-48f)
      quadToRelative(0f, -17f, -11.5f, -28.5f)
      reflectiveQuadTo(480f, 320f)
      quadToRelative(-10f, 0f, -18.5f, 4f)
      reflectiveQuadTo(448f, 336f)
      lineToRelative(72f, 72f)
      close()
      moveTo(480f, 880f)
      quadToRelative(-74f, 0f, -139.5f, -28.5f)
      reflectiveQuadTo(226f, 774f)
      quadToRelative(-49f, -49f, -77.5f, -114.5f)
      reflectiveQuadTo(120f, 520f)
      quadToRelative(0f, -60f, 18.5f, -115.5f)
      reflectiveQuadTo(192f, 304f)
      lineTo(84f, 196f)
      quadToRelative(-11f, -11f, -11f, -28f)
      reflectiveQuadToRelative(11f, -28f)
      quadToRelative(11f, -11f, 28f, -11f)
      reflectiveQuadToRelative(28f, 11f)
      lineToRelative(680f, 680f)
      quadToRelative(11f, 11f, 11f, 28f)
      reflectiveQuadToRelative(-11f, 28f)
      quadToRelative(-11f, 11f, -28f, 11f)
      reflectiveQuadToRelative(-28f, -11f)
      lineToRelative(-68f, -68f)
      quadToRelative(-48f, 35f, -103.5f, 53.5f)
      reflectiveQuadTo(480f, 880f)
      close()
      moveTo(480f, 800f)
      quadToRelative(42f, 0f, 82f, -13f)
      reflectiveQuadToRelative(75f, -37f)
      lineTo(248f, 361f)
      quadToRelative(-24f, 35f, -36f, 75f)
      reflectiveQuadToRelative(-12f, 84f)
      quadToRelative(0f, 116f, 82f, 198f)
      reflectiveQuadToRelative(198f, 82f)
      close()
      moveTo(400f, 120f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(360f, 80f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(400f, 40f)
      horizontalLineToRelative(160f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(600f, 80f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(560f, 120f)
      lineTo(400f, 120f)
      close()
      moveTo(443f, 555f)
      close()
      moveTo(556f, 443f)
      close()
    }
  }.build()
}

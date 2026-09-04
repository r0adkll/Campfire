// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.Replay10: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "Replay10",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(360f, 460f)
      horizontalLineToRelative(-30f)
      quadToRelative(-13f, 0f, -21.5f, -8.5f)
      reflectiveQuadTo(300f, 430f)
      quadToRelative(0f, -13f, 8.5f, -21.5f)
      reflectiveQuadTo(330f, 400f)
      horizontalLineToRelative(60f)
      quadToRelative(13f, 0f, 21.5f, 8.5f)
      reflectiveQuadTo(420f, 430f)
      verticalLineToRelative(180f)
      quadToRelative(0f, 13f, -8.5f, 21.5f)
      reflectiveQuadTo(390f, 640f)
      quadToRelative(-13f, 0f, -21.5f, -8.5f)
      reflectiveQuadTo(360f, 610f)
      verticalLineToRelative(-150f)
      close()
      moveTo(500f, 640f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(460f, 600f)
      verticalLineToRelative(-160f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(500f, 400f)
      horizontalLineToRelative(80f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(620f, 440f)
      verticalLineToRelative(160f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(580f, 640f)
      horizontalLineToRelative(-80f)
      close()
      moveTo(520f, 580f)
      horizontalLineToRelative(40f)
      verticalLineToRelative(-120f)
      horizontalLineToRelative(-40f)
      verticalLineToRelative(120f)
      close()
      moveTo(339.5f, 851.5f)
      quadToRelative(-65.5f, -28.5f, -114f, -77f)
      reflectiveQuadToRelative(-77f, -114f)
      quadTo(120f, 595f, 120f, 520f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(160f, 480f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(200f, 520f)
      quadToRelative(0f, 117f, 81.5f, 198.5f)
      reflectiveQuadTo(480f, 800f)
      quadToRelative(117f, 0f, 198.5f, -81.5f)
      reflectiveQuadTo(760f, 520f)
      quadToRelative(0f, -117f, -81.5f, -198.5f)
      reflectiveQuadTo(480f, 240f)
      horizontalLineToRelative(-6f)
      lineToRelative(34f, 34f)
      quadToRelative(12f, 12f, 11.5f, 28f)
      reflectiveQuadTo(508f, 330f)
      quadToRelative(-12f, 12f, -28.5f, 12.5f)
      reflectiveQuadTo(451f, 331f)
      lineTo(348f, 228f)
      quadToRelative(-12f, -12f, -12f, -28f)
      reflectiveQuadToRelative(12f, -28f)
      lineToRelative(103f, -103f)
      quadToRelative(12f, -12f, 28.5f, -11.5f)
      reflectiveQuadTo(508f, 70f)
      quadToRelative(11f, 12f, 11.5f, 28f)
      reflectiveQuadTo(508f, 126f)
      lineToRelative(-34f, 34f)
      horizontalLineToRelative(6f)
      quadToRelative(75f, 0f, 140.5f, 28.5f)
      reflectiveQuadToRelative(114f, 77f)
      quadToRelative(48.5f, 48.5f, 77f, 114f)
      reflectiveQuadTo(840f, 520f)
      quadToRelative(0f, 75f, -28.5f, 140.5f)
      reflectiveQuadToRelative(-77f, 114f)
      quadToRelative(-48.5f, 48.5f, -114f, 77f)
      reflectiveQuadTo(480f, 880f)
      quadToRelative(-75f, 0f, -140.5f, -28.5f)
      close()
    }
  }.build()
}

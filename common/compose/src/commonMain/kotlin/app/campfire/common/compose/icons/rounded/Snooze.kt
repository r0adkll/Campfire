// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.Snooze: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "Snooze",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveToRelative(468f, 566f)
      lineToRelative(107f, -120f)
      quadToRelative(1f, -1f, 5f, -14f)
      verticalLineToRelative(-16f)
      quadToRelative(0f, -13f, -8.5f, -21.5f)
      reflectiveQuadTo(550f, 386f)
      lineTo(410f, 386f)
      quadToRelative(-13f, 0f, -21.5f, 8.5f)
      reflectiveQuadTo(380f, 416f)
      quadToRelative(0f, 13f, 8.5f, 21.5f)
      reflectiveQuadTo(410f, 446f)
      horizontalLineToRelative(84f)
      lineTo(385f, 568f)
      quadToRelative(-1f, 1f, -5f, 14f)
      verticalLineToRelative(14f)
      quadToRelative(0f, 13f, 8.5f, 21.5f)
      reflectiveQuadTo(410f, 626f)
      horizontalLineToRelative(140f)
      quadToRelative(13f, 0f, 21.5f, -8.5f)
      reflectiveQuadTo(580f, 596f)
      quadToRelative(0f, -13f, -8.5f, -21.5f)
      reflectiveQuadTo(550f, 566f)
      horizontalLineToRelative(-82f)
      close()
      moveTo(339.5f, 851.5f)
      quadToRelative(-65.5f, -28.5f, -114f, -77f)
      reflectiveQuadToRelative(-77f, -114f)
      quadTo(120f, 595f, 120f, 520f)
      reflectiveQuadToRelative(28.5f, -140.5f)
      quadToRelative(28.5f, -65.5f, 77f, -114f)
      reflectiveQuadToRelative(114f, -77f)
      quadTo(405f, 160f, 480f, 160f)
      reflectiveQuadToRelative(140.5f, 28.5f)
      quadToRelative(65.5f, 28.5f, 114f, 77f)
      reflectiveQuadToRelative(77f, 114f)
      quadTo(840f, 445f, 840f, 520f)
      reflectiveQuadToRelative(-28.5f, 140.5f)
      quadToRelative(-28.5f, 65.5f, -77f, 114f)
      reflectiveQuadToRelative(-114f, 77f)
      quadTo(555f, 880f, 480f, 880f)
      reflectiveQuadToRelative(-140.5f, -28.5f)
      close()
      moveTo(480f, 520f)
      close()
      moveTo(82f, 292f)
      quadToRelative(-11f, -11f, -11f, -28f)
      reflectiveQuadToRelative(11f, -28f)
      lineToRelative(114f, -114f)
      quadToRelative(11f, -11f, 28f, -11f)
      reflectiveQuadToRelative(28f, 11f)
      quadToRelative(11f, 11f, 11f, 28f)
      reflectiveQuadToRelative(-11f, 28f)
      lineTo(138f, 292f)
      quadToRelative(-11f, 11f, -28f, 11f)
      reflectiveQuadToRelative(-28f, -11f)
      close()
      moveTo(878f, 292f)
      quadToRelative(-11f, 11f, -28f, 11f)
      reflectiveQuadToRelative(-28f, -11f)
      lineTo(708f, 178f)
      quadToRelative(-11f, -11f, -11f, -28f)
      reflectiveQuadToRelative(11f, -28f)
      quadToRelative(11f, -11f, 28f, -11f)
      reflectiveQuadToRelative(28f, 11f)
      lineToRelative(114f, 114f)
      quadToRelative(11f, 11f, 11f, 28f)
      reflectiveQuadToRelative(-11f, 28f)
      close()
      moveTo(480f, 800f)
      quadToRelative(117f, 0f, 198.5f, -81.5f)
      reflectiveQuadTo(760f, 520f)
      quadToRelative(0f, -117f, -81.5f, -198.5f)
      reflectiveQuadTo(480f, 240f)
      quadToRelative(-117f, 0f, -198.5f, 81.5f)
      reflectiveQuadTo(200f, 520f)
      quadToRelative(0f, 117f, 81.5f, 198.5f)
      reflectiveQuadTo(480f, 800f)
      close()
    }
  }.build()
}

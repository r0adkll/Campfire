// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.FlashlightOff: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "FlashlightOff",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(764f, 876f)
      lineTo(640f, 752f)
      verticalLineToRelative(48f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(560f, 880f)
      lineTo(400f, 880f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(320f, 800f)
      verticalLineToRelative(-368f)
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
      close()
      moveTo(400f, 800f)
      horizontalLineToRelative(160f)
      verticalLineToRelative(-128f)
      lineTo(400f, 512f)
      verticalLineToRelative(288f)
      close()
      moveTo(720f, 160f)
      verticalLineToRelative(136f)
      quadToRelative(0f, 12f, -3f, 23f)
      reflectiveQuadToRelative(-10f, 21f)
      lineToRelative(-74f, 113f)
      quadToRelative(-11f, 17f, -26.5f, 18f)
      reflectiveQuadToRelative(-28.5f, -7f)
      quadToRelative(-6f, -4f, -10.5f, -10f)
      reflectiveQuadToRelative(-6.5f, -13.5f)
      quadToRelative(-2f, -7.5f, -0.5f, -15.5f)
      reflectiveQuadToRelative(6.5f, -16f)
      lineToRelative(73f, -113f)
      verticalLineToRelative(-16f)
      lineTo(410f, 280f)
      quadToRelative(-17f, 0f, -28f, -11.5f)
      reflectiveQuadTo(371f, 240f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(411f, 200f)
      horizontalLineToRelative(229f)
      verticalLineToRelative(-40f)
      lineTo(289f, 160f)
      quadToRelative(-17f, 0f, -28f, -11.5f)
      reflectiveQuadTo(250f, 120f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(290f, 80f)
      horizontalLineToRelative(350f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(720f, 160f)
      close()
      moveTo(480f, 592f)
      close()
      moveTo(410f, 280f)
      close()
    }
  }.build()
}

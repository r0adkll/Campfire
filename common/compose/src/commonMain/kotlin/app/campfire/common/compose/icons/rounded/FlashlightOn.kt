// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.FlashlightOn: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "FlashlightOn",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(320f, 800f)
      verticalLineToRelative(-360f)
      lineToRelative(-67f, -100f)
      quadToRelative(-7f, -10f, -10f, -21f)
      reflectiveQuadToRelative(-3f, -23f)
      verticalLineToRelative(-136f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(320f, 80f)
      horizontalLineToRelative(320f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(720f, 160f)
      verticalLineToRelative(136f)
      quadToRelative(0f, 12f, -3f, 23f)
      reflectiveQuadToRelative(-10f, 21f)
      lineToRelative(-67f, 100f)
      verticalLineToRelative(360f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(560f, 880f)
      lineTo(400f, 880f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(320f, 800f)
      close()
      moveTo(437.5f, 602.5f)
      quadTo(420f, 585f, 420f, 560f)
      reflectiveQuadToRelative(17.5f, -42.5f)
      quadTo(455f, 500f, 480f, 500f)
      reflectiveQuadToRelative(42.5f, 17.5f)
      quadTo(540f, 535f, 540f, 560f)
      reflectiveQuadToRelative(-17.5f, 42.5f)
      quadTo(505f, 620f, 480f, 620f)
      reflectiveQuadToRelative(-42.5f, -17.5f)
      close()
      moveTo(320f, 200f)
      horizontalLineToRelative(320f)
      verticalLineToRelative(-40f)
      lineTo(320f, 160f)
      verticalLineToRelative(40f)
      close()
      moveTo(640f, 280f)
      lineTo(320f, 280f)
      verticalLineToRelative(16f)
      lineToRelative(80f, 120f)
      verticalLineToRelative(384f)
      horizontalLineToRelative(160f)
      verticalLineToRelative(-384f)
      lineToRelative(80f, -120f)
      verticalLineToRelative(-16f)
      close()
      moveTo(480f, 480f)
      close()
    }
  }.build()
}

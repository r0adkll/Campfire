// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.EditSquare: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "EditSquare",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(200f, 840f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(120f, 760f)
      verticalLineToRelative(-560f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(200f, 120f)
      horizontalLineToRelative(261f)
      quadToRelative(20f, 0f, 30f, 12.5f)
      reflectiveQuadToRelative(10f, 27.5f)
      quadToRelative(0f, 15f, -10.5f, 27.5f)
      reflectiveQuadTo(460f, 200f)
      lineTo(200f, 200f)
      verticalLineToRelative(560f)
      horizontalLineToRelative(560f)
      verticalLineToRelative(-261f)
      quadToRelative(0f, -20f, 12.5f, -30f)
      reflectiveQuadToRelative(27.5f, -10f)
      quadToRelative(15f, 0f, 27.5f, 10f)
      reflectiveQuadToRelative(12.5f, 30f)
      verticalLineToRelative(261f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(760f, 840f)
      lineTo(200f, 840f)
      close()
      moveTo(480f, 480f)
      close()
      moveTo(360f, 560f)
      verticalLineToRelative(-97f)
      quadToRelative(0f, -16f, 6f, -30.5f)
      reflectiveQuadToRelative(17f, -25.5f)
      lineToRelative(344f, -344f)
      quadToRelative(12f, -12f, 27f, -18f)
      reflectiveQuadToRelative(30f, -6f)
      quadToRelative(16f, 0f, 30.5f, 6f)
      reflectiveQuadToRelative(26.5f, 18f)
      lineToRelative(56f, 57f)
      quadToRelative(11f, 12f, 17f, 26.5f)
      reflectiveQuadToRelative(6f, 29.5f)
      quadToRelative(0f, 15f, -5.5f, 29.5f)
      reflectiveQuadTo(897f, 232f)
      lineTo(553f, 576f)
      quadToRelative(-11f, 11f, -25.5f, 17.5f)
      reflectiveQuadTo(497f, 600f)
      horizontalLineToRelative(-97f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(360f, 560f)
      close()
      moveTo(841f, 176f)
      lineTo(785f, 120f)
      lineTo(841f, 176f)
      close()
      moveTo(440f, 520f)
      horizontalLineToRelative(56f)
      lineToRelative(232f, -232f)
      lineToRelative(-28f, -28f)
      lineToRelative(-29f, -28f)
      lineToRelative(-231f, 231f)
      verticalLineToRelative(57f)
      close()
      moveTo(700f, 260f)
      lineTo(671f, 232f)
      lineTo(700f, 260f)
      lineTo(728f, 288f)
      lineTo(700f, 260f)
      close()
    }
  }.build()
}

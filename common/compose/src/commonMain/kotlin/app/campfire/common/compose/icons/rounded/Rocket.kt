// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.Rocket: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "Rocket",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(319f, 730f)
      quadToRelative(-10f, -29f, -18.5f, -59f)
      reflectiveQuadTo(287f, 611f)
      lineToRelative(-47f, 32f)
      verticalLineToRelative(119f)
      lineToRelative(79f, -32f)
      close()
      moveTo(466f, 195f)
      quadToRelative(-48f, 51f, -77f, 134.5f)
      reflectiveQuadTo(360f, 505f)
      quadToRelative(0f, 60f, 11f, 117.5f)
      reflectiveQuadToRelative(29f, 97.5f)
      horizontalLineToRelative(160f)
      quadToRelative(18f, -40f, 29f, -97.5f)
      reflectiveQuadTo(600f, 505f)
      quadToRelative(0f, -92f, -29f, -175.5f)
      reflectiveQuadTo(494f, 195f)
      quadToRelative(-3f, -3f, -6.5f, -4.5f)
      reflectiveQuadTo(480f, 189f)
      quadToRelative(-4f, 0f, -7.5f, 1.5f)
      reflectiveQuadTo(466f, 195f)
      close()
      moveTo(480f, 520f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(400f, 440f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(480f, 360f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(560f, 440f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(480f, 520f)
      close()
      moveTo(641f, 730f)
      lineTo(720f, 762f)
      verticalLineToRelative(-119f)
      lineToRelative(-47f, -32f)
      quadToRelative(-5f, 30f, -13.5f, 60f)
      reflectiveQuadTo(641f, 730f)
      close()
      moveTo(511f, 104f)
      quadToRelative(84f, 72f, 126.5f, 177f)
      reflectiveQuadTo(680f, 520f)
      lineToRelative(84f, 56f)
      quadToRelative(17f, 11f, 26.5f, 29f)
      reflectiveQuadToRelative(9.5f, 38f)
      verticalLineToRelative(178f)
      quadToRelative(0f, 21f, -17.5f, 33f)
      reflectiveQuadToRelative(-37.5f, 4f)
      lineToRelative(-144f, -58f)
      lineTo(359f, 800f)
      lineToRelative(-144f, 58f)
      quadToRelative(-20f, 8f, -37.5f, -4f)
      reflectiveQuadTo(160f, 821f)
      verticalLineToRelative(-178f)
      quadToRelative(0f, -20f, 9.5f, -38f)
      reflectiveQuadToRelative(26.5f, -29f)
      lineToRelative(84f, -56f)
      quadToRelative(0f, -134f, 42.5f, -239f)
      reflectiveQuadTo(449f, 104f)
      quadToRelative(7f, -5f, 15f, -8f)
      reflectiveQuadToRelative(16f, -3f)
      quadToRelative(8f, 0f, 16f, 3f)
      reflectiveQuadToRelative(15f, 8f)
      close()
    }
  }.build()
}

// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.Hearing: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "Hearing",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(760f, 360f)
      quadToRelative(0f, 63f, -20.5f, 119.5f)
      reflectiveQuadTo(682f, 583f)
      quadToRelative(-11f, 14f, -11f, 30.5f)
      reflectiveQuadToRelative(12f, 28.5f)
      quadToRelative(13f, 12f, 30f, 10.5f)
      reflectiveQuadToRelative(28f, -15.5f)
      quadToRelative(46f, -57f, 72.5f, -128f)
      reflectiveQuadTo(840f, 360f)
      quadToRelative(0f, -80f, -26.5f, -151f)
      reflectiveQuadTo(741f, 81f)
      quadToRelative(-11f, -14f, -28f, -15.5f)
      reflectiveQuadTo(683f, 76f)
      quadToRelative(-12f, 12f, -12f, 28.5f)
      reflectiveQuadToRelative(11f, 30.5f)
      quadToRelative(37f, 47f, 57.5f, 104f)
      reflectiveQuadTo(760f, 360f)
      close()
      moveTo(471f, 430.5f)
      quadToRelative(29f, -29.5f, 29f, -70.5f)
      quadToRelative(0f, -42f, -29f, -71f)
      reflectiveQuadToRelative(-71f, -29f)
      quadToRelative(-42f, 0f, -71f, 29f)
      reflectiveQuadToRelative(-29f, 71f)
      quadToRelative(0f, 41f, 29f, 70.5f)
      reflectiveQuadToRelative(71f, 29.5f)
      quadToRelative(42f, 0f, 71f, -29.5f)
      close()
      moveTo(200f, 720f)
      quadToRelative(0f, -17f, -11.5f, -28.5f)
      reflectiveQuadTo(160f, 680f)
      quadToRelative(-17f, 0f, -28.5f, 11.5f)
      reflectiveQuadTo(120f, 720f)
      quadToRelative(0f, 66f, 47f, 113f)
      reflectiveQuadToRelative(113f, 47f)
      quadToRelative(62f, 0f, 101.5f, -31f)
      reflectiveQuadToRelative(60.5f, -91f)
      quadToRelative(17f, -50f, 32.5f, -70f)
      reflectiveQuadToRelative(71.5f, -64f)
      quadToRelative(62f, -50f, 98f, -113f)
      reflectiveQuadToRelative(36f, -151f)
      quadToRelative(0f, -119f, -80.5f, -199.5f)
      reflectiveQuadTo(400f, 80f)
      quadToRelative(-119f, 0f, -199.5f, 80.5f)
      reflectiveQuadTo(120f, 360f)
      quadToRelative(0f, 17f, 11.5f, 28.5f)
      reflectiveQuadTo(160f, 400f)
      quadToRelative(17f, 0f, 28.5f, -11.5f)
      reflectiveQuadTo(200f, 360f)
      quadToRelative(0f, -85f, 57.5f, -142.5f)
      reflectiveQuadTo(400f, 160f)
      quadToRelative(85f, 0f, 142.5f, 57.5f)
      reflectiveQuadTo(600f, 360f)
      quadToRelative(0f, 68f, -27f, 116f)
      reflectiveQuadToRelative(-77f, 86f)
      quadToRelative(-52f, 38f, -81f, 74f)
      reflectiveQuadToRelative(-43f, 78f)
      quadToRelative(-14f, 44f, -33.5f, 65f)
      reflectiveQuadTo(280f, 800f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(200f, 720f)
      close()
    }
  }.build()
}

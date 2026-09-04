// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.filled

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Filled.QueryStats: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "QueryStats",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(70f, 536f)
      quadToRelative(-13f, -9f, -15.5f, -24.5f)
      reflectiveQuadTo(60f, 482f)
      lineToRelative(122f, -196f)
      quadToRelative(22f, -35f, 62.5f, -37.5f)
      reflectiveQuadTo(311f, 277f)
      lineToRelative(49f, 57f)
      lineToRelative(95f, -154f)
      quadToRelative(23f, -38f, 66.5f, -38.5f)
      reflectiveQuadTo(589f, 178f)
      lineToRelative(51f, 76f)
      lineToRelative(112f, -178f)
      quadToRelative(9f, -15f, 26.5f, -18.5f)
      reflectiveQuadTo(810f, 65f)
      quadToRelative(13f, 9f, 15.5f, 24.5f)
      reflectiveQuadTo(820f, 119f)
      lineTo(708f, 297f)
      quadToRelative(-23f, 37f, -66.5f, 37f)
      reflectiveQuadTo(574f, 298f)
      lineToRelative(-51f, -76f)
      lineToRelative(-95f, 154f)
      quadToRelative(-21f, 35f, -61.5f, 38f)
      reflectiveQuadTo(300f, 386f)
      lineToRelative(-50f, -58f)
      lineToRelative(-122f, 197f)
      quadToRelative(-9f, 15f, -26.5f, 18.5f)
      reflectiveQuadTo(70f, 536f)
      close()
      moveTo(580f, 720f)
      quadToRelative(42f, 0f, 71f, -29f)
      reflectiveQuadToRelative(29f, -71f)
      quadToRelative(0f, -42f, -29f, -71f)
      reflectiveQuadToRelative(-71f, -29f)
      quadToRelative(-42f, 0f, -71f, 29f)
      reflectiveQuadToRelative(-29f, 71f)
      quadToRelative(0f, 42f, 29f, 71f)
      reflectiveQuadToRelative(71f, 29f)
      close()
      moveTo(580f, 800f)
      quadToRelative(-75f, 0f, -127.5f, -52.5f)
      reflectiveQuadTo(400f, 620f)
      quadToRelative(0f, -75f, 52.5f, -127.5f)
      reflectiveQuadTo(580f, 440f)
      quadToRelative(75f, 0f, 127.5f, 52.5f)
      reflectiveQuadTo(760f, 620f)
      quadToRelative(0f, 26f, -7f, 50.5f)
      reflectiveQuadTo(732f, 716f)
      lineToRelative(80f, 80f)
      quadToRelative(11f, 11f, 11f, 28f)
      reflectiveQuadToRelative(-11f, 28f)
      quadToRelative(-11f, 11f, -28f, 11f)
      reflectiveQuadToRelative(-28f, -11f)
      lineToRelative(-80f, -80f)
      quadToRelative(-21f, 14f, -45.5f, 21f)
      reflectiveQuadToRelative(-50.5f, 7f)
      close()
    }
  }.build()
}

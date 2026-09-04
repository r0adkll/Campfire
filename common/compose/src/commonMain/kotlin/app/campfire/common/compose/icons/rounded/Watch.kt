// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.Watch: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "Watch",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(420f, 160f)
      horizontalLineToRelative(120f)
      horizontalLineToRelative(-120f)
      close()
      moveTo(420f, 800f)
      horizontalLineToRelative(120f)
      horizontalLineToRelative(-120f)
      close()
      moveTo(420f, 880f)
      quadToRelative(-26f, 0f, -47.5f, -15.5f)
      reflectiveQuadTo(343f, 823f)
      lineToRelative(-23f, -77f)
      quadToRelative(-6f, -20f, -18.5f, -40.5f)
      reflectiveQuadTo(269f, 663f)
      quadToRelative(-34f, -37f, -51.5f, -84f)
      reflectiveQuadTo(200f, 480f)
      quadToRelative(0f, -51f, 17.5f, -98f)
      reflectiveQuadToRelative(51.5f, -85f)
      quadToRelative(20f, -23f, 32.5f, -43f)
      reflectiveQuadToRelative(18.5f, -40f)
      lineToRelative(23f, -77f)
      quadToRelative(8f, -26f, 29.5f, -41.5f)
      reflectiveQuadTo(420f, 80f)
      horizontalLineToRelative(120f)
      quadToRelative(26f, 0f, 47.5f, 15.5f)
      reflectiveQuadTo(617f, 137f)
      lineToRelative(23f, 77f)
      quadToRelative(6f, 20f, 18.5f, 40.5f)
      reflectiveQuadTo(691f, 297f)
      quadToRelative(34f, 37f, 51.5f, 84f)
      reflectiveQuadToRelative(17.5f, 99f)
      quadToRelative(0f, 51f, -17.5f, 98f)
      reflectiveQuadTo(691f, 663f)
      quadToRelative(-20f, 23f, -32.5f, 43f)
      reflectiveQuadTo(640f, 746f)
      lineToRelative(-23f, 77f)
      quadToRelative(-8f, 26f, -29.5f, 41.5f)
      reflectiveQuadTo(540f, 880f)
      lineTo(420f, 880f)
      close()
      moveTo(621.5f, 621.5f)
      quadTo(680f, 563f, 680f, 480f)
      reflectiveQuadToRelative(-58.5f, -141.5f)
      quadTo(563f, 280f, 480f, 280f)
      reflectiveQuadToRelative(-141.5f, 58.5f)
      quadTo(280f, 397f, 280f, 480f)
      reflectiveQuadToRelative(58.5f, 141.5f)
      quadTo(397f, 680f, 480f, 680f)
      reflectiveQuadToRelative(141.5f, -58.5f)
      close()
      moveTo(404f, 210f)
      quadToRelative(20f, -5f, 38.5f, -8f)
      reflectiveQuadToRelative(37.5f, -3f)
      quadToRelative(19f, 0f, 37.5f, 3f)
      reflectiveQuadToRelative(38.5f, 8f)
      lineToRelative(-16f, -50f)
      lineTo(420f, 160f)
      lineToRelative(-16f, 50f)
      close()
      moveTo(420f, 800f)
      horizontalLineToRelative(120f)
      lineToRelative(16f, -50f)
      quadToRelative(-20f, 5f, -38.5f, 7.5f)
      reflectiveQuadTo(480f, 760f)
      quadToRelative(-19f, 0f, -37.5f, -2.5f)
      reflectiveQuadTo(404f, 750f)
      lineToRelative(16f, 50f)
      close()
    }
  }.build()
}

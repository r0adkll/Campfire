// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.PhotoAlbum: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "PhotoAlbum",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(240f, 880f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(160f, 800f)
      verticalLineToRelative(-640f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(240f, 80f)
      horizontalLineToRelative(480f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(800f, 160f)
      verticalLineToRelative(640f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(720f, 880f)
      lineTo(240f, 880f)
      close()
      moveTo(240f, 800f)
      horizontalLineToRelative(480f)
      verticalLineToRelative(-640f)
      horizontalLineToRelative(-80f)
      verticalLineToRelative(245f)
      quadToRelative(0f, 12f, -10f, 17.5f)
      reflectiveQuadToRelative(-20f, -0.5f)
      lineToRelative(-49f, -30f)
      quadToRelative(-10f, -6f, -20.5f, -6f)
      reflectiveQuadToRelative(-20.5f, 6f)
      lineToRelative(-49f, 30f)
      quadToRelative(-10f, 6f, -20.5f, 0.5f)
      reflectiveQuadTo(440f, 405f)
      verticalLineToRelative(-245f)
      lineTo(240f, 160f)
      verticalLineToRelative(640f)
      close()
      moveTo(440f, 680f)
      lineTo(391f, 614f)
      quadToRelative(-6f, -8f, -16f, -8f)
      reflectiveQuadToRelative(-16f, 8f)
      lineToRelative(-55f, 74f)
      quadToRelative(-8f, 10f, -2f, 21f)
      reflectiveQuadToRelative(18f, 11f)
      horizontalLineToRelative(320f)
      quadToRelative(12f, 0f, 18f, -11f)
      reflectiveQuadToRelative(-2f, -21f)
      lineToRelative(-95f, -127f)
      quadToRelative(-6f, -8f, -16f, -8f)
      reflectiveQuadToRelative(-16f, 8f)
      lineToRelative(-89f, 119f)
      close()
      moveTo(240f, 800f)
      verticalLineToRelative(-640f)
      verticalLineToRelative(640f)
      close()
      moveTo(440f, 405f)
      quadToRelative(0f, 12f, 10.5f, 17.5f)
      reflectiveQuadToRelative(20.5f, -0.5f)
      lineToRelative(49f, -30f)
      quadToRelative(10f, -6f, 20.5f, -6f)
      reflectiveQuadToRelative(20.5f, 6f)
      lineToRelative(49f, 30f)
      quadToRelative(10f, 6f, 20f, 0.5f)
      reflectiveQuadToRelative(10f, -17.5f)
      quadToRelative(0f, 12f, -10f, 17.5f)
      reflectiveQuadToRelative(-20f, -0.5f)
      lineToRelative(-49f, -30f)
      quadToRelative(-10f, -6f, -20.5f, -6f)
      reflectiveQuadToRelative(-20.5f, 6f)
      lineToRelative(-49f, 30f)
      quadToRelative(-10f, 6f, -20.5f, 0.5f)
      reflectiveQuadTo(440f, 405f)
      close()
    }
  }.build()
}

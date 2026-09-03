// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.Computer: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "Computer",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(80f, 840f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(40f, 800f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(80f, 760f)
      horizontalLineToRelative(800f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(920f, 800f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(880f, 840f)
      lineTo(80f, 840f)
      close()
      moveTo(160f, 720f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(80f, 640f)
      verticalLineToRelative(-440f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(160f, 120f)
      horizontalLineToRelative(640f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(880f, 200f)
      verticalLineToRelative(440f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(800f, 720f)
      lineTo(160f, 720f)
      close()
      moveTo(160f, 640f)
      horizontalLineToRelative(640f)
      verticalLineToRelative(-440f)
      lineTo(160f, 200f)
      verticalLineToRelative(440f)
      close()
      moveTo(160f, 640f)
      verticalLineToRelative(-440f)
      verticalLineToRelative(440f)
      close()
    }
  }.build()
}

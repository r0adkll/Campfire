// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.Lan: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "Lan",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(120f, 800f)
      verticalLineToRelative(-120f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(200f, 600f)
      horizontalLineToRelative(40f)
      verticalLineToRelative(-80f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(320f, 440f)
      horizontalLineToRelative(120f)
      verticalLineToRelative(-80f)
      horizontalLineToRelative(-40f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(320f, 280f)
      verticalLineToRelative(-120f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(400f, 80f)
      horizontalLineToRelative(160f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(640f, 160f)
      verticalLineToRelative(120f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(560f, 360f)
      horizontalLineToRelative(-40f)
      verticalLineToRelative(80f)
      horizontalLineToRelative(120f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(720f, 520f)
      verticalLineToRelative(80f)
      horizontalLineToRelative(40f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(840f, 680f)
      verticalLineToRelative(120f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(760f, 880f)
      lineTo(600f, 880f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(520f, 800f)
      verticalLineToRelative(-120f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(600f, 600f)
      horizontalLineToRelative(40f)
      verticalLineToRelative(-80f)
      lineTo(320f, 520f)
      verticalLineToRelative(80f)
      horizontalLineToRelative(40f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(440f, 680f)
      verticalLineToRelative(120f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(360f, 880f)
      lineTo(200f, 880f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(120f, 800f)
      close()
      moveTo(400f, 280f)
      horizontalLineToRelative(160f)
      verticalLineToRelative(-120f)
      lineTo(400f, 160f)
      verticalLineToRelative(120f)
      close()
      moveTo(200f, 800f)
      horizontalLineToRelative(160f)
      verticalLineToRelative(-120f)
      lineTo(200f, 680f)
      verticalLineToRelative(120f)
      close()
      moveTo(600f, 800f)
      horizontalLineToRelative(160f)
      verticalLineToRelative(-120f)
      lineTo(600f, 680f)
      verticalLineToRelative(120f)
      close()
      moveTo(480f, 280f)
      close()
      moveTo(360f, 680f)
      close()
      moveTo(600f, 680f)
      close()
    }
  }.build()
}

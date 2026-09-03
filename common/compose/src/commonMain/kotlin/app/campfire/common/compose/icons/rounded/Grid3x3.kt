// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.Grid3X3: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "Rounded.Grid3X3",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(320f, 640f)
      lineTo(200f, 640f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(160f, 600f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(200f, 560f)
      horizontalLineToRelative(120f)
      verticalLineToRelative(-160f)
      lineTo(200f, 400f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(160f, 360f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(200f, 320f)
      horizontalLineToRelative(120f)
      verticalLineToRelative(-120f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(360f, 160f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(400f, 200f)
      verticalLineToRelative(120f)
      horizontalLineToRelative(160f)
      verticalLineToRelative(-120f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(600f, 160f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(640f, 200f)
      verticalLineToRelative(120f)
      horizontalLineToRelative(120f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(800f, 360f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(760f, 400f)
      lineTo(640f, 400f)
      verticalLineToRelative(160f)
      horizontalLineToRelative(120f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(800f, 600f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(760f, 640f)
      lineTo(640f, 640f)
      verticalLineToRelative(120f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(600f, 800f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(560f, 760f)
      verticalLineToRelative(-120f)
      lineTo(400f, 640f)
      verticalLineToRelative(120f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(360f, 800f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(320f, 760f)
      verticalLineToRelative(-120f)
      close()
      moveTo(400f, 560f)
      horizontalLineToRelative(160f)
      verticalLineToRelative(-160f)
      lineTo(400f, 400f)
      verticalLineToRelative(160f)
      close()
    }
  }.build()
}

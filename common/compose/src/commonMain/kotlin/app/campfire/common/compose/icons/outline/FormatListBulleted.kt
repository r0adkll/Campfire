// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.outline

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Outlined.FormatListBulleted: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "FormatListBulleted",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(400f, 760f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(360f, 720f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(400f, 680f)
      horizontalLineToRelative(400f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(840f, 720f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(800f, 760f)
      lineTo(400f, 760f)
      close()
      moveTo(400f, 520f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(360f, 480f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(400f, 440f)
      horizontalLineToRelative(400f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(840f, 480f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(800f, 520f)
      lineTo(400f, 520f)
      close()
      moveTo(400f, 280f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(360f, 240f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(400f, 200f)
      horizontalLineToRelative(400f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(840f, 240f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(800f, 280f)
      lineTo(400f, 280f)
      close()
      moveTo(200f, 800f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(120f, 720f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(200f, 640f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(280f, 720f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(200f, 800f)
      close()
      moveTo(200f, 560f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(120f, 480f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(200f, 400f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(280f, 480f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(200f, 560f)
      close()
      moveTo(143.5f, 296.5f)
      quadTo(120f, 273f, 120f, 240f)
      reflectiveQuadToRelative(23.5f, -56.5f)
      quadTo(167f, 160f, 200f, 160f)
      reflectiveQuadToRelative(56.5f, 23.5f)
      quadTo(280f, 207f, 280f, 240f)
      reflectiveQuadToRelative(-23.5f, 56.5f)
      quadTo(233f, 320f, 200f, 320f)
      reflectiveQuadToRelative(-56.5f, -23.5f)
      close()
    }
  }.build()
}

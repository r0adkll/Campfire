// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.FormatPaint: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "FormatPaint",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(440f, 880f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(360f, 800f)
      verticalLineToRelative(-160f)
      lineTo(240f, 640f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(160f, 560f)
      verticalLineToRelative(-280f)
      quadToRelative(0f, -66f, 47f, -113f)
      reflectiveQuadToRelative(113f, -47f)
      horizontalLineToRelative(440f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(800f, 160f)
      verticalLineToRelative(400f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(720f, 640f)
      lineTo(600f, 640f)
      verticalLineToRelative(160f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(520f, 880f)
      horizontalLineToRelative(-80f)
      close()
      moveTo(240f, 400f)
      horizontalLineToRelative(480f)
      verticalLineToRelative(-200f)
      horizontalLineToRelative(-40f)
      verticalLineToRelative(120f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(640f, 360f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(600f, 320f)
      verticalLineToRelative(-120f)
      horizontalLineToRelative(-40f)
      verticalLineToRelative(40f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(520f, 280f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(480f, 240f)
      verticalLineToRelative(-40f)
      lineTo(320f, 200f)
      quadToRelative(-33f, 0f, -56.5f, 23.5f)
      reflectiveQuadTo(240f, 280f)
      verticalLineToRelative(120f)
      close()
      moveTo(240f, 560f)
      horizontalLineToRelative(480f)
      verticalLineToRelative(-80f)
      lineTo(240f, 480f)
      verticalLineToRelative(80f)
      close()
      moveTo(240f, 560f)
      verticalLineToRelative(-80f)
      verticalLineToRelative(80f)
      close()
    }
  }.build()
}

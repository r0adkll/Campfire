// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.OpenInFull: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "OpenInFull",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(160f, 840f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(120f, 800f)
      verticalLineToRelative(-240f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(160f, 520f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(200f, 560f)
      verticalLineToRelative(144f)
      lineToRelative(504f, -504f)
      lineTo(560f, 200f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(520f, 160f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(560f, 120f)
      horizontalLineToRelative(240f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(840f, 160f)
      verticalLineToRelative(240f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(800f, 440f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(760f, 400f)
      verticalLineToRelative(-144f)
      lineTo(256f, 760f)
      horizontalLineToRelative(144f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(440f, 800f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(400f, 840f)
      lineTo(160f, 840f)
      close()
    }
  }.build()
}

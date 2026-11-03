// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.CloseFullscreen: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "CloseFullscreen",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(400f, 616f)
      lineTo(164f, 852f)
      quadToRelative(-11f, 11f, -28f, 11f)
      reflectiveQuadToRelative(-28f, -11f)
      quadToRelative(-11f, -11f, -11f, -28f)
      reflectiveQuadToRelative(11f, -28f)
      lineToRelative(236f, -236f)
      lineTo(200f, 560f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(160f, 520f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(200f, 480f)
      horizontalLineToRelative(240f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(480f, 520f)
      verticalLineToRelative(240f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(440f, 800f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(400f, 760f)
      verticalLineToRelative(-144f)
      close()
      moveTo(616f, 400f)
      horizontalLineToRelative(144f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(800f, 440f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(760f, 480f)
      lineTo(520f, 480f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(480f, 440f)
      verticalLineToRelative(-240f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(520f, 160f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(560f, 200f)
      verticalLineToRelative(144f)
      lineToRelative(236f, -236f)
      quadToRelative(11f, -11f, 28f, -11f)
      reflectiveQuadToRelative(28f, 11f)
      quadToRelative(11f, 11f, 11f, 28f)
      reflectiveQuadToRelative(-11f, 28f)
      lineTo(616f, 400f)
      close()
    }
  }.build()
}

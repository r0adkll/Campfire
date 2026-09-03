// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.ArrowForward: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "ArrowForward",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
    autoMirror = true,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(647f, 520f)
      lineTo(200f, 520f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(160f, 480f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(200f, 440f)
      horizontalLineToRelative(447f)
      lineTo(451f, 244f)
      quadToRelative(-12f, -12f, -11.5f, -28f)
      reflectiveQuadToRelative(12.5f, -28f)
      quadToRelative(12f, -11f, 28f, -11.5f)
      reflectiveQuadToRelative(28f, 11.5f)
      lineToRelative(264f, 264f)
      quadToRelative(6f, 6f, 8.5f, 13f)
      reflectiveQuadToRelative(2.5f, 15f)
      quadToRelative(0f, 8f, -2.5f, 15f)
      reflectiveQuadToRelative(-8.5f, 13f)
      lineTo(508f, 772f)
      quadToRelative(-11f, 11f, -27.5f, 11f)
      reflectiveQuadTo(452f, 772f)
      quadToRelative(-12f, -12f, -12f, -28.5f)
      reflectiveQuadToRelative(12f, -28.5f)
      lineToRelative(195f, -195f)
      close()
    }
  }.build()
}

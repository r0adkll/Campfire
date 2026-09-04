// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.TrendingFlat: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "TrendingFlat",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
    autoMirror = true,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(727f, 520f)
      lineTo(160f, 520f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(120f, 480f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(160f, 440f)
      horizontalLineToRelative(567f)
      lineToRelative(-55f, -56f)
      quadToRelative(-12f, -12f, -11.5f, -28f)
      reflectiveQuadToRelative(12.5f, -28f)
      quadToRelative(12f, -11f, 28.5f, -11.5f)
      reflectiveQuadTo(729f, 328f)
      lineToRelative(123f, 124f)
      quadToRelative(12f, 12f, 12f, 28f)
      reflectiveQuadToRelative(-12f, 28f)
      lineTo(728f, 632f)
      quadToRelative(-11f, 11f, -27.5f, 11f)
      reflectiveQuadTo(672f, 632f)
      quadToRelative(-12f, -12f, -12f, -28.5f)
      reflectiveQuadToRelative(12f, -28.5f)
      lineToRelative(55f, -55f)
      close()
    }
  }.build()
}

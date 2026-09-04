// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.filled

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Filled.CloudDownload: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "CloudDownload",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(260f, 800f)
      quadToRelative(-91f, 0f, -155.5f, -63f)
      reflectiveQuadTo(40f, 583f)
      quadToRelative(0f, -78f, 47f, -139f)
      reflectiveQuadToRelative(123f, -78f)
      quadToRelative(23f, -81f, 85.5f, -136f)
      reflectiveQuadTo(440f, 163f)
      verticalLineToRelative(323f)
      lineToRelative(-36f, -35f)
      quadToRelative(-11f, -11f, -27.5f, -11f)
      reflectiveQuadTo(348f, 452f)
      quadToRelative(-11f, 11f, -11f, 28f)
      reflectiveQuadToRelative(11f, 28f)
      lineToRelative(104f, 104f)
      quadToRelative(12f, 12f, 28f, 12f)
      reflectiveQuadToRelative(28f, -12f)
      lineToRelative(104f, -104f)
      quadToRelative(11f, -11f, 11.5f, -27.5f)
      reflectiveQuadTo(612f, 452f)
      quadToRelative(-11f, -11f, -27.5f, -11.5f)
      reflectiveQuadTo(556f, 451f)
      lineToRelative(-36f, 35f)
      verticalLineToRelative(-323f)
      quadToRelative(103f, 14f, 171.5f, 92.5f)
      reflectiveQuadTo(760f, 440f)
      quadToRelative(69f, 8f, 114.5f, 59.5f)
      reflectiveQuadTo(920f, 620f)
      quadToRelative(0f, 75f, -52.5f, 127.5f)
      reflectiveQuadTo(740f, 800f)
      lineTo(260f, 800f)
      close()
    }
  }.build()
}

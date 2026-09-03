// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.UnfoldLess: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "UnfoldLess",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveToRelative(480f, 676f)
      lineToRelative(-96f, 96f)
      quadToRelative(-11f, 11f, -28f, 11f)
      reflectiveQuadToRelative(-28f, -11f)
      quadToRelative(-11f, -11f, -11f, -28f)
      reflectiveQuadToRelative(11f, -28f)
      lineToRelative(124f, -124f)
      quadToRelative(6f, -6f, 13f, -8.5f)
      reflectiveQuadToRelative(15f, -2.5f)
      quadToRelative(8f, 0f, 15f, 2.5f)
      reflectiveQuadToRelative(13f, 8.5f)
      lineToRelative(124f, 124f)
      quadToRelative(11f, 11f, 11f, 28f)
      reflectiveQuadToRelative(-11f, 28f)
      quadToRelative(-11f, 11f, -28f, 11f)
      reflectiveQuadToRelative(-28f, -11f)
      lineToRelative(-96f, -96f)
      close()
      moveTo(480f, 284f)
      lineTo(576f, 188f)
      quadToRelative(11f, -11f, 28f, -11f)
      reflectiveQuadToRelative(28f, 11f)
      quadToRelative(11f, 11f, 11f, 28f)
      reflectiveQuadToRelative(-11f, 28f)
      lineTo(508f, 368f)
      quadToRelative(-6f, 6f, -13f, 8.5f)
      reflectiveQuadToRelative(-15f, 2.5f)
      quadToRelative(-8f, 0f, -15f, -2.5f)
      reflectiveQuadToRelative(-13f, -8.5f)
      lineTo(328f, 244f)
      quadToRelative(-11f, -11f, -11f, -28f)
      reflectiveQuadToRelative(11f, -28f)
      quadToRelative(11f, -11f, 28f, -11f)
      reflectiveQuadToRelative(28f, 11f)
      lineToRelative(96f, 96f)
      close()
    }
  }.build()
}

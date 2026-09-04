// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.UnfoldMore: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "UnfoldMore",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveToRelative(480f, 724f)
      lineToRelative(93f, -93f)
      quadToRelative(12f, -12f, 29f, -12f)
      reflectiveQuadToRelative(29f, 12f)
      quadToRelative(12f, 12f, 12f, 29f)
      reflectiveQuadToRelative(-12f, 29f)
      lineTo(508f, 812f)
      quadToRelative(-6f, 6f, -13f, 8.5f)
      reflectiveQuadToRelative(-15f, 2.5f)
      quadToRelative(-8f, 0f, -15f, -2.5f)
      reflectiveQuadToRelative(-13f, -8.5f)
      lineTo(329f, 689f)
      quadToRelative(-12f, -12f, -12f, -29f)
      reflectiveQuadToRelative(12f, -29f)
      quadToRelative(12f, -12f, 29f, -12f)
      reflectiveQuadToRelative(29f, 12f)
      lineToRelative(93f, 93f)
      close()
      moveTo(480f, 240f)
      lineTo(387f, 333f)
      quadToRelative(-12f, 12f, -29f, 12f)
      reflectiveQuadToRelative(-29f, -12f)
      quadToRelative(-12f, -12f, -12f, -29f)
      reflectiveQuadToRelative(12f, -29f)
      lineToRelative(123f, -123f)
      quadToRelative(6f, -6f, 13f, -8.5f)
      reflectiveQuadToRelative(15f, -2.5f)
      quadToRelative(8f, 0f, 15f, 2.5f)
      reflectiveQuadToRelative(13f, 8.5f)
      lineToRelative(123f, 123f)
      quadToRelative(12f, 12f, 12f, 29f)
      reflectiveQuadToRelative(-12f, 29f)
      quadToRelative(-12f, 12f, -29f, 12f)
      reflectiveQuadToRelative(-29f, -12f)
      lineToRelative(-93f, -93f)
      close()
    }
  }.build()
}

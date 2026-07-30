// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.ExpandAllSemiBold: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "ExpandAllSemiBold",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color(0xFFE8EAED))) {
      moveToRelative(480f, 748.48f)
      lineToRelative(146.52f, -145.96f)
      quadToRelative(15.39f, -15.39f, 37.05f, -15.39f)
      quadToRelative(21.65f, 0f, 37.04f, 15.39f)
      reflectiveQuadTo(716f, 639.78f)
      quadToRelative(0f, 21.87f, -15.39f, 37.26f)
      lineTo(555.09f, 823f)
      quadToRelative(-30.92f, 30.91f, -75.09f, 30.91f)
      reflectiveQuadTo(404.91f, 823f)
      lineTo(258.96f, 677.04f)
      quadToRelative(-15.4f, -15.39f, -15.18f, -37.26f)
      quadToRelative(0.22f, -21.87f, 15.61f, -37.26f)
      quadToRelative(15.39f, -15.39f, 37.26f, -15.39f)
      quadToRelative(21.87f, 0f, 37.26f, 15.39f)
      lineTo(480f, 748.48f)
      close()
      moveTo(480f, 211.52f)
      lineTo(333.91f, 357.04f)
      quadToRelative(-15.39f, 15.4f, -37.04f, 15.18f)
      quadToRelative(-21.65f, -0.22f, -37.04f, -15.18f)
      quadToRelative(-15.4f, -15.39f, -15.61f, -37.26f)
      quadToRelative(-0.22f, -21.87f, 15.17f, -37.26f)
      lineTo(404.91f, 137f)
      quadToRelative(30.92f, -30.91f, 75.09f, -30.91f)
      reflectiveQuadTo(555.09f, 137f)
      lineToRelative(145.52f, 145.52f)
      quadToRelative(15.39f, 15.39f, 15.17f, 37.26f)
      quadToRelative(-0.21f, 21.87f, -15.61f, 37.26f)
      quadToRelative(-15.39f, 14.96f, -37.04f, 15.18f)
      quadToRelative(-21.65f, 0.22f, -37.04f, -15.18f)
      lineTo(480f, 211.52f)
      close()
    }
  }.build()
}

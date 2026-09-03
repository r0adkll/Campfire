// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.ArrowDropDown: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "ArrowDropDown",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(459f, 579f)
      lineTo(314f, 434f)
      quadToRelative(-3f, -3f, -4.5f, -6.5f)
      reflectiveQuadTo(308f, 420f)
      quadToRelative(0f, -8f, 5.5f, -14f)
      reflectiveQuadToRelative(14.5f, -6f)
      horizontalLineToRelative(304f)
      quadToRelative(9f, 0f, 14.5f, 6f)
      reflectiveQuadToRelative(5.5f, 14f)
      quadToRelative(0f, 2f, -6f, 14f)
      lineTo(501f, 579f)
      quadToRelative(-5f, 5f, -10f, 7f)
      reflectiveQuadToRelative(-11f, 2f)
      quadToRelative(-6f, 0f, -11f, -2f)
      reflectiveQuadToRelative(-10f, -7f)
      close()
    }
  }.build()
}

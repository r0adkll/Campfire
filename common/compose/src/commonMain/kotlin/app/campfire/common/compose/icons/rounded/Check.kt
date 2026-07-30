// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.Check: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "Check",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveToRelative(382f, 606f)
      lineToRelative(339f, -339f)
      quadToRelative(12f, -12f, 28f, -12f)
      reflectiveQuadToRelative(28f, 12f)
      quadToRelative(12f, 12f, 12f, 28.5f)
      reflectiveQuadTo(777f, 324f)
      lineTo(410f, 692f)
      quadToRelative(-12f, 12f, -28f, 12f)
      reflectiveQuadToRelative(-28f, -12f)
      lineTo(182f, 520f)
      quadToRelative(-12f, -12f, -11.5f, -28.5f)
      reflectiveQuadTo(183f, 463f)
      quadToRelative(12f, -12f, 28.5f, -12f)
      reflectiveQuadToRelative(28.5f, 12f)
      lineToRelative(142f, 143f)
      close()
    }
  }.build()
}

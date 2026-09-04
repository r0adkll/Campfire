// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.Backspace: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "Backspace",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
    autoMirror = true,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveToRelative(560f, 536f)
      lineToRelative(76f, 76f)
      quadToRelative(11f, 11f, 28f, 11f)
      reflectiveQuadToRelative(28f, -11f)
      quadToRelative(11f, -11f, 11f, -28f)
      reflectiveQuadToRelative(-11f, -28f)
      lineToRelative(-76f, -76f)
      lineToRelative(76f, -76f)
      quadToRelative(11f, -11f, 11f, -28f)
      reflectiveQuadToRelative(-11f, -28f)
      quadToRelative(-11f, -11f, -28f, -11f)
      reflectiveQuadToRelative(-28f, 11f)
      lineToRelative(-76f, 76f)
      lineToRelative(-76f, -76f)
      quadToRelative(-11f, -11f, -28f, -11f)
      reflectiveQuadToRelative(-28f, 11f)
      quadToRelative(-11f, 11f, -11f, 28f)
      reflectiveQuadToRelative(11f, 28f)
      lineToRelative(76f, 76f)
      lineToRelative(-76f, 76f)
      quadToRelative(-11f, 11f, -11f, 28f)
      reflectiveQuadToRelative(11f, 28f)
      quadToRelative(11f, 11f, 28f, 11f)
      reflectiveQuadToRelative(28f, -11f)
      lineToRelative(76f, -76f)
      close()
      moveTo(360f, 800f)
      quadToRelative(-19f, 0f, -36f, -8.5f)
      reflectiveQuadTo(296f, 768f)
      lineTo(116f, 528f)
      quadToRelative(-16f, -21f, -16f, -48f)
      reflectiveQuadToRelative(16f, -48f)
      lineToRelative(180f, -240f)
      quadToRelative(11f, -15f, 28f, -23.5f)
      reflectiveQuadToRelative(36f, -8.5f)
      horizontalLineToRelative(440f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(880f, 240f)
      verticalLineToRelative(480f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(800f, 800f)
      lineTo(360f, 800f)
      close()
      moveTo(360f, 720f)
      horizontalLineToRelative(440f)
      verticalLineToRelative(-480f)
      lineTo(360f, 240f)
      lineTo(180f, 480f)
      lineToRelative(180f, 240f)
      close()
      moveTo(490f, 480f)
      close()
    }
  }.build()
}

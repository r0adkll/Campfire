// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.Stop: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "Stop",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(240f, 640f)
      verticalLineToRelative(-320f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(320f, 240f)
      horizontalLineToRelative(320f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(720f, 320f)
      verticalLineToRelative(320f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(640f, 720f)
      lineTo(320f, 720f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(240f, 640f)
      close()
    }
  }.build()
}

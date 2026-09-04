// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.Tablet: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "Tablet",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(120f, 800f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(40f, 720f)
      verticalLineToRelative(-480f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(120f, 160f)
      horizontalLineToRelative(720f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(920f, 240f)
      verticalLineToRelative(480f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(840f, 800f)
      lineTo(120f, 800f)
      close()
      moveTo(160f, 240f)
      horizontalLineToRelative(-40f)
      verticalLineToRelative(480f)
      horizontalLineToRelative(40f)
      verticalLineToRelative(-480f)
      close()
      moveTo(240f, 720f)
      horizontalLineToRelative(480f)
      verticalLineToRelative(-480f)
      lineTo(240f, 240f)
      verticalLineToRelative(480f)
      close()
      moveTo(800f, 240f)
      verticalLineToRelative(480f)
      horizontalLineToRelative(40f)
      verticalLineToRelative(-480f)
      horizontalLineToRelative(-40f)
      close()
      moveTo(800f, 240f)
      horizontalLineToRelative(40f)
      horizontalLineToRelative(-40f)
      close()
      moveTo(160f, 240f)
      horizontalLineToRelative(-40f)
      horizontalLineToRelative(40f)
      close()
    }
  }.build()
}

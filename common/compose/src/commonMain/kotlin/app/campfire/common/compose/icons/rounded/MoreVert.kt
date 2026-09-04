// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.MoreVert: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "MoreVert",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(480f, 800f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(400f, 720f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(480f, 640f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(560f, 720f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(480f, 800f)
      close()
      moveTo(480f, 560f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(400f, 480f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(480f, 400f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(560f, 480f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(480f, 560f)
      close()
      moveTo(480f, 320f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(400f, 240f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(480f, 160f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(560f, 240f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(480f, 320f)
      close()
    }
  }.build()
}

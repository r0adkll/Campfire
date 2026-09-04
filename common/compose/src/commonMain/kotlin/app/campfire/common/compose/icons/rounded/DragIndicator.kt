// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.DragIndicator: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "DragIndicator",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(360f, 800f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(280f, 720f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(360f, 640f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(440f, 720f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(360f, 800f)
      close()
      moveTo(600f, 800f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(520f, 720f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(600f, 640f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(680f, 720f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(600f, 800f)
      close()
      moveTo(360f, 560f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(280f, 480f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(360f, 400f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(440f, 480f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(360f, 560f)
      close()
      moveTo(600f, 560f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(520f, 480f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(600f, 400f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(680f, 480f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(600f, 560f)
      close()
      moveTo(360f, 320f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(280f, 240f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(360f, 160f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(440f, 240f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(360f, 320f)
      close()
      moveTo(600f, 320f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(520f, 240f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(600f, 160f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(680f, 240f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(600f, 320f)
      close()
    }
  }.build()
}

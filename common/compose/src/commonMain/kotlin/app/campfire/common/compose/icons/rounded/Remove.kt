// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.Remove: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "Remove",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(240f, 520f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(200f, 480f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(240f, 440f)
      horizontalLineToRelative(480f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(760f, 480f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(720f, 520f)
      lineTo(240f, 520f)
      close()
    }
  }.build()
}

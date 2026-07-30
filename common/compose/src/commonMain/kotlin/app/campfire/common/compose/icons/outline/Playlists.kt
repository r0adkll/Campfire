// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.outline

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Outlined.Playlists: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "Playlists",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(280f, 520f)
      horizontalLineToRelative(280f)
      quadToRelative(17f, 0f, 28.5f, -11.5f)
      reflectiveQuadTo(600f, 480f)
      quadToRelative(0f, -17f, -11.5f, -28.5f)
      reflectiveQuadTo(560f, 440f)
      lineTo(280f, 440f)
      quadToRelative(-17f, 0f, -28.5f, 11.5f)
      reflectiveQuadTo(240f, 480f)
      quadToRelative(0f, 17f, 11.5f, 28.5f)
      reflectiveQuadTo(280f, 520f)
      close()
      moveTo(280f, 400f)
      horizontalLineToRelative(280f)
      quadToRelative(17f, 0f, 28.5f, -11.5f)
      reflectiveQuadTo(600f, 360f)
      quadToRelative(0f, -17f, -11.5f, -28.5f)
      reflectiveQuadTo(560f, 320f)
      lineTo(280f, 320f)
      quadToRelative(-17f, 0f, -28.5f, 11.5f)
      reflectiveQuadTo(240f, 360f)
      quadToRelative(0f, 17f, 11.5f, 28.5f)
      reflectiveQuadTo(280f, 400f)
      close()
      moveTo(160f, 800f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(80f, 720f)
      verticalLineToRelative(-480f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(160f, 160f)
      horizontalLineToRelative(640f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(880f, 240f)
      verticalLineToRelative(480f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(800f, 800f)
      lineTo(160f, 800f)
      close()
      moveTo(160f, 720f)
      horizontalLineToRelative(640f)
      verticalLineToRelative(-480f)
      lineTo(160f, 240f)
      verticalLineToRelative(480f)
      close()
      moveTo(160f, 720f)
      verticalLineToRelative(-480f)
      verticalLineToRelative(480f)
      close()
    }
  }.build()
}

// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.FilterAltOff: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "FilterAltOff",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveToRelative(592f, 479f)
      lineToRelative(-57f, -57f)
      lineToRelative(143f, -182f)
      lineTo(353f, 240f)
      lineToRelative(-80f, -80f)
      horizontalLineToRelative(487f)
      quadToRelative(25f, 0f, 36f, 22f)
      reflectiveQuadToRelative(-4f, 42f)
      lineTo(592f, 479f)
      close()
      moveTo(560f, 673f)
      verticalLineToRelative(87f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(520f, 800f)
      horizontalLineToRelative(-80f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(400f, 760f)
      verticalLineToRelative(-247f)
      lineTo(84f, 197f)
      quadToRelative(-11f, -11f, -11f, -27.5f)
      reflectiveQuadTo(84f, 141f)
      quadToRelative(12f, -12f, 28.5f, -12f)
      reflectiveQuadToRelative(28.5f, 12f)
      lineToRelative(679f, 679f)
      quadToRelative(12f, 12f, 11.5f, 28f)
      reflectiveQuadTo(819f, 876f)
      quadToRelative(-12f, 11f, -28f, 11.5f)
      reflectiveQuadTo(763f, 876f)
      lineTo(560f, 673f)
      close()
      moveTo(535f, 422f)
      close()
    }
  }.build()
}

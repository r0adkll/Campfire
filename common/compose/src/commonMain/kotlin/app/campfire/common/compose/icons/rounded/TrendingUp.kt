// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.TrendingUp: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "TrendingUp",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
    autoMirror = true,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(108f, 705f)
      quadToRelative(-12f, -12f, -11.5f, -28.5f)
      reflectiveQuadTo(108f, 649f)
      lineToRelative(211f, -214f)
      quadToRelative(23f, -23f, 57f, -23f)
      reflectiveQuadToRelative(57f, 23f)
      lineToRelative(103f, 104f)
      lineToRelative(208f, -206f)
      horizontalLineToRelative(-64f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(640f, 293f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(680f, 253f)
      horizontalLineToRelative(160f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(880f, 293f)
      verticalLineToRelative(160f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(840f, 493f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(800f, 453f)
      verticalLineToRelative(-64f)
      lineTo(593f, 596f)
      quadToRelative(-23f, 23f, -57f, 23f)
      reflectiveQuadToRelative(-57f, -23f)
      lineTo(376f, 493f)
      lineTo(164f, 705f)
      quadToRelative(-11f, 11f, -28f, 11f)
      reflectiveQuadToRelative(-28f, -11f)
      close()
    }
  }.build()
}

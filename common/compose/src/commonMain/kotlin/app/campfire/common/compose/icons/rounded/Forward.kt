// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.Forward: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "Forward",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
    autoMirror = true,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(767f, 440f)
      lineTo(612f, 285f)
      quadToRelative(-12f, -12f, -12f, -28.5f)
      reflectiveQuadToRelative(12f, -28.5f)
      quadToRelative(12f, -11f, 28.5f, -11f)
      reflectiveQuadToRelative(27.5f, 11f)
      lineToRelative(184f, 184f)
      quadToRelative(6f, 6f, 8.5f, 13f)
      reflectiveQuadToRelative(2.5f, 15f)
      quadToRelative(0f, 8f, -2.5f, 15f)
      reflectiveQuadToRelative(-8.5f, 13f)
      lineTo(668f, 652f)
      quadToRelative(-12f, 12f, -28f, 11.5f)
      reflectiveQuadTo(612f, 652f)
      quadToRelative(-12f, -12f, -12.5f, -28f)
      reflectiveQuadToRelative(11.5f, -28f)
      lineToRelative(156f, -156f)
      close()
      moveTo(527f, 480f)
      lineTo(280f, 480f)
      quadToRelative(-50f, 0f, -85f, 35f)
      reflectiveQuadToRelative(-35f, 85f)
      verticalLineToRelative(120f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(120f, 760f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(80f, 720f)
      verticalLineToRelative(-120f)
      quadToRelative(0f, -83f, 58.5f, -141.5f)
      reflectiveQuadTo(280f, 400f)
      horizontalLineToRelative(247f)
      lineTo(412f, 285f)
      quadToRelative(-12f, -12f, -12f, -28.5f)
      reflectiveQuadToRelative(12f, -28.5f)
      quadToRelative(12f, -11f, 28.5f, -11f)
      reflectiveQuadToRelative(27.5f, 11f)
      lineToRelative(184f, 184f)
      quadToRelative(6f, 6f, 8.5f, 13f)
      reflectiveQuadToRelative(2.5f, 15f)
      quadToRelative(0f, 8f, -2.5f, 15f)
      reflectiveQuadToRelative(-8.5f, 13f)
      lineTo(468f, 652f)
      quadToRelative(-12f, 12f, -28f, 11.5f)
      reflectiveQuadTo(412f, 652f)
      quadToRelative(-12f, -12f, -12.5f, -28f)
      reflectiveQuadToRelative(11.5f, -28f)
      lineToRelative(116f, -116f)
      close()
    }
  }.build()
}

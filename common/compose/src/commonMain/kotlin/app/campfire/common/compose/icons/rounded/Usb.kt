// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.Usb: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "Usb",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(423.5f, 856.5f)
      quadTo(400f, 833f, 400f, 800f)
      quadToRelative(0f, -21f, 11f, -39f)
      reflectiveQuadToRelative(29f, -29f)
      verticalLineToRelative(-92f)
      lineTo(320f, 640f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(240f, 560f)
      verticalLineToRelative(-92f)
      quadToRelative(-18f, -9f, -29f, -27f)
      reflectiveQuadToRelative(-11f, -41f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(280f, 320f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(360f, 400f)
      quadToRelative(0f, 23f, -11f, 40f)
      reflectiveQuadToRelative(-29f, 28f)
      verticalLineToRelative(92f)
      horizontalLineToRelative(120f)
      verticalLineToRelative(-320f)
      horizontalLineToRelative(-40f)
      quadToRelative(-12f, 0f, -18f, -11f)
      reflectiveQuadToRelative(2f, -21f)
      lineToRelative(80f, -107f)
      quadToRelative(6f, -8f, 16f, -8f)
      reflectiveQuadToRelative(16f, 8f)
      lineToRelative(80f, 107f)
      quadToRelative(8f, 10f, 2f, 21f)
      reflectiveQuadToRelative(-18f, 11f)
      horizontalLineToRelative(-40f)
      verticalLineToRelative(320f)
      horizontalLineToRelative(120f)
      verticalLineToRelative(-80f)
      horizontalLineToRelative(-20f)
      quadToRelative(-8f, 0f, -14f, -6f)
      reflectiveQuadToRelative(-6f, -14f)
      verticalLineToRelative(-120f)
      quadToRelative(0f, -8f, 6f, -14f)
      reflectiveQuadToRelative(14f, -6f)
      horizontalLineToRelative(120f)
      quadToRelative(8f, 0f, 14f, 6f)
      reflectiveQuadToRelative(6f, 14f)
      verticalLineToRelative(120f)
      quadToRelative(0f, 8f, -6f, 14f)
      reflectiveQuadToRelative(-14f, 6f)
      horizontalLineToRelative(-20f)
      verticalLineToRelative(80f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(640f, 640f)
      lineTo(520f, 640f)
      verticalLineToRelative(92f)
      quadToRelative(19f, 10f, 29.5f, 28f)
      reflectiveQuadToRelative(10.5f, 40f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(480f, 880f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      close()
    }
  }.build()
}

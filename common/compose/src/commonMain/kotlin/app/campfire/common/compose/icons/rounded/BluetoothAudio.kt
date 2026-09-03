// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.BluetoothAudio: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "BluetoothAudio",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(360f, 576f)
      lineTo(204f, 732f)
      quadToRelative(-11f, 11f, -28f, 11f)
      reflectiveQuadToRelative(-28f, -11f)
      quadToRelative(-11f, -11f, -11f, -28f)
      reflectiveQuadToRelative(11f, -28f)
      lineToRelative(196f, -196f)
      lineToRelative(-196f, -196f)
      quadToRelative(-11f, -11f, -11f, -28f)
      reflectiveQuadToRelative(11f, -28f)
      quadToRelative(11f, -11f, 28f, -11f)
      reflectiveQuadToRelative(28f, 11f)
      lineToRelative(156f, 156f)
      verticalLineToRelative(-247f)
      quadToRelative(0f, -18f, 12f, -29.5f)
      reflectiveQuadToRelative(28f, -11.5f)
      quadToRelative(8f, 0f, 15f, 3f)
      reflectiveQuadToRelative(13f, 9f)
      lineToRelative(172f, 172f)
      quadToRelative(6f, 6f, 8.5f, 13f)
      reflectiveQuadToRelative(2.5f, 15f)
      quadToRelative(0f, 8f, -2.5f, 15f)
      reflectiveQuadToRelative(-8.5f, 13f)
      lineTo(456f, 480f)
      lineToRelative(144f, 144f)
      quadToRelative(6f, 6f, 8.5f, 13f)
      reflectiveQuadToRelative(2.5f, 15f)
      quadToRelative(0f, 8f, -2.5f, 15f)
      reflectiveQuadToRelative(-8.5f, 13f)
      lineTo(428f, 852f)
      quadToRelative(-6f, 6f, -13f, 9f)
      reflectiveQuadToRelative(-15f, 3f)
      quadToRelative(-16f, 0f, -28f, -11.5f)
      reflectiveQuadTo(360f, 823f)
      verticalLineToRelative(-247f)
      close()
      moveTo(440f, 384f)
      lineTo(516f, 308f)
      lineTo(440f, 234f)
      verticalLineToRelative(150f)
      close()
      moveTo(440f, 726f)
      lineTo(516f, 652f)
      lineTo(440f, 576f)
      verticalLineToRelative(150f)
      close()
      moveTo(634f, 545f)
      lineTo(584f, 494f)
      quadToRelative(-6f, -6f, -6f, -14f)
      reflectiveQuadToRelative(6f, -14f)
      lineToRelative(50f, -50f)
      quadToRelative(12f, -12f, 24.5f, -9f)
      reflectiveQuadToRelative(16.5f, 19f)
      quadToRelative(4f, 14f, 5.5f, 27f)
      reflectiveQuadToRelative(1.5f, 27f)
      quadToRelative(0f, 14f, -1.5f, 28f)
      reflectiveQuadToRelative(-5.5f, 28f)
      quadToRelative(-4f, 16f, -16.5f, 18.5f)
      reflectiveQuadTo(634f, 545f)
      close()
      moveTo(747f, 657f)
      quadToRelative(-7f, -7f, -9f, -18f)
      reflectiveQuadToRelative(3f, -20f)
      quadToRelative(15f, -32f, 23f, -67.5f)
      reflectiveQuadToRelative(8f, -71.5f)
      quadToRelative(0f, -36f, -8f, -71f)
      reflectiveQuadToRelative(-23f, -67f)
      quadToRelative(-5f, -10f, -3f, -21f)
      reflectiveQuadToRelative(10f, -19f)
      quadToRelative(14f, -14f, 31f, -10f)
      reflectiveQuadToRelative(25f, 23f)
      quadToRelative(18f, 39f, 27f, 80.5f)
      reflectiveQuadToRelative(9f, 84.5f)
      quadToRelative(0f, 43f, -9.5f, 84f)
      reflectiveQuadTo(804f, 643f)
      quadToRelative(-8f, 19f, -25.5f, 23.5f)
      reflectiveQuadTo(747f, 657f)
      close()
    }
  }.build()
}

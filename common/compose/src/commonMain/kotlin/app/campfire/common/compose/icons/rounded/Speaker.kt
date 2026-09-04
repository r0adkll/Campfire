// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.Speaker: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "Speaker",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(680f, 880f)
      lineTo(280f, 880f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(200f, 800f)
      verticalLineToRelative(-640f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(280f, 80f)
      horizontalLineToRelative(400f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(760f, 160f)
      verticalLineToRelative(640f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(680f, 880f)
      close()
      moveTo(680f, 800f)
      verticalLineToRelative(-640f)
      lineTo(280f, 160f)
      verticalLineToRelative(640f)
      horizontalLineToRelative(400f)
      close()
      moveTo(536.5f, 336.5f)
      quadTo(560f, 313f, 560f, 280f)
      reflectiveQuadToRelative(-23.5f, -56.5f)
      quadTo(513f, 200f, 480f, 200f)
      reflectiveQuadToRelative(-56.5f, 23.5f)
      quadTo(400f, 247f, 400f, 280f)
      reflectiveQuadToRelative(23.5f, 56.5f)
      quadTo(447f, 360f, 480f, 360f)
      reflectiveQuadToRelative(56.5f, -23.5f)
      close()
      moveTo(593f, 713f)
      quadToRelative(47f, -47f, 47f, -113f)
      reflectiveQuadToRelative(-47f, -113f)
      quadToRelative(-47f, -47f, -113f, -47f)
      reflectiveQuadToRelative(-113f, 47f)
      quadToRelative(-47f, 47f, -47f, 113f)
      reflectiveQuadToRelative(47f, 113f)
      quadToRelative(47f, 47f, 113f, 47f)
      reflectiveQuadToRelative(113f, -47f)
      close()
      moveTo(423.5f, 656.5f)
      quadTo(400f, 633f, 400f, 600f)
      reflectiveQuadToRelative(23.5f, -56.5f)
      quadTo(447f, 520f, 480f, 520f)
      reflectiveQuadToRelative(56.5f, 23.5f)
      quadTo(560f, 567f, 560f, 600f)
      reflectiveQuadToRelative(-23.5f, 56.5f)
      quadTo(513f, 680f, 480f, 680f)
      reflectiveQuadToRelative(-56.5f, -23.5f)
      close()
      moveTo(280f, 160f)
      verticalLineToRelative(640f)
      verticalLineToRelative(-640f)
      close()
    }
  }.build()
}

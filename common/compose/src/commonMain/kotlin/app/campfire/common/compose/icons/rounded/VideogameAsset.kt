// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.VideogameAsset: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "VideogameAsset",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(160f, 720f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(80f, 640f)
      verticalLineToRelative(-320f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(160f, 240f)
      horizontalLineToRelative(640f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(880f, 320f)
      verticalLineToRelative(320f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(800f, 720f)
      lineTo(160f, 720f)
      close()
      moveTo(160f, 640f)
      horizontalLineToRelative(640f)
      verticalLineToRelative(-320f)
      lineTo(160f, 320f)
      verticalLineToRelative(320f)
      close()
      moveTo(280f, 520f)
      verticalLineToRelative(40f)
      quadToRelative(0f, 17f, 11.5f, 28.5f)
      reflectiveQuadTo(320f, 600f)
      quadToRelative(17f, 0f, 28.5f, -11.5f)
      reflectiveQuadTo(360f, 560f)
      verticalLineToRelative(-40f)
      horizontalLineToRelative(40f)
      quadToRelative(17f, 0f, 28.5f, -11.5f)
      reflectiveQuadTo(440f, 480f)
      quadToRelative(0f, -17f, -11.5f, -28.5f)
      reflectiveQuadTo(400f, 440f)
      horizontalLineToRelative(-40f)
      verticalLineToRelative(-40f)
      quadToRelative(0f, -17f, -11.5f, -28.5f)
      reflectiveQuadTo(320f, 360f)
      quadToRelative(-17f, 0f, -28.5f, 11.5f)
      reflectiveQuadTo(280f, 400f)
      verticalLineToRelative(40f)
      horizontalLineToRelative(-40f)
      quadToRelative(-17f, 0f, -28.5f, 11.5f)
      reflectiveQuadTo(200f, 480f)
      quadToRelative(0f, 17f, 11.5f, 28.5f)
      reflectiveQuadTo(240f, 520f)
      horizontalLineToRelative(40f)
      close()
      moveTo(622.5f, 582.5f)
      quadTo(640f, 565f, 640f, 540f)
      reflectiveQuadToRelative(-17.5f, -42.5f)
      quadTo(605f, 480f, 580f, 480f)
      reflectiveQuadToRelative(-42.5f, 17.5f)
      quadTo(520f, 515f, 520f, 540f)
      reflectiveQuadToRelative(17.5f, 42.5f)
      quadTo(555f, 600f, 580f, 600f)
      reflectiveQuadToRelative(42.5f, -17.5f)
      close()
      moveTo(742.5f, 462.5f)
      quadTo(760f, 445f, 760f, 420f)
      reflectiveQuadToRelative(-17.5f, -42.5f)
      quadTo(725f, 360f, 700f, 360f)
      reflectiveQuadToRelative(-42.5f, 17.5f)
      quadTo(640f, 395f, 640f, 420f)
      reflectiveQuadToRelative(17.5f, 42.5f)
      quadTo(675f, 480f, 700f, 480f)
      reflectiveQuadToRelative(42.5f, -17.5f)
      close()
      moveTo(160f, 640f)
      verticalLineToRelative(-320f)
      verticalLineToRelative(320f)
      close()
    }
  }.build()
}

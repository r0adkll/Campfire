// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.FitScreen: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "FitScreen",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(800f, 320f)
      verticalLineToRelative(-80f)
      horizontalLineToRelative(-80f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(680f, 200f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(720f, 160f)
      horizontalLineToRelative(80f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(880f, 240f)
      verticalLineToRelative(80f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(840f, 360f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(800f, 320f)
      close()
      moveTo(80f, 320f)
      verticalLineToRelative(-80f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(160f, 160f)
      horizontalLineToRelative(80f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(280f, 200f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(240f, 240f)
      horizontalLineToRelative(-80f)
      verticalLineToRelative(80f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(120f, 360f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(80f, 320f)
      close()
      moveTo(800f, 800f)
      horizontalLineToRelative(-80f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(680f, 760f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(720f, 720f)
      horizontalLineToRelative(80f)
      verticalLineToRelative(-80f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(840f, 600f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(880f, 640f)
      verticalLineToRelative(80f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(800f, 800f)
      close()
      moveTo(160f, 800f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(80f, 720f)
      verticalLineToRelative(-80f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(120f, 600f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(160f, 640f)
      verticalLineToRelative(80f)
      horizontalLineToRelative(80f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(280f, 760f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(240f, 800f)
      horizontalLineToRelative(-80f)
      close()
      moveTo(240f, 560f)
      verticalLineToRelative(-160f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(320f, 320f)
      horizontalLineToRelative(320f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(720f, 400f)
      verticalLineToRelative(160f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(640f, 640f)
      lineTo(320f, 640f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(240f, 560f)
      close()
      moveTo(320f, 560f)
      horizontalLineToRelative(320f)
      verticalLineToRelative(-160f)
      lineTo(320f, 400f)
      verticalLineToRelative(160f)
      close()
      moveTo(320f, 560f)
      verticalLineToRelative(-160f)
      verticalLineToRelative(160f)
      close()
    }
  }.build()
}

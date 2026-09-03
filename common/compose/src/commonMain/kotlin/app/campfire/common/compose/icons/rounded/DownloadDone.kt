// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.DownloadDone: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "DownloadDone",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveToRelative(382f, 526f)
      lineToRelative(338f, -338f)
      quadToRelative(12f, -12f, 28.5f, -12f)
      reflectiveQuadToRelative(28.5f, 12f)
      quadToRelative(12f, 12f, 12f, 28.5f)
      reflectiveQuadTo(777f, 245f)
      lineTo(410f, 612f)
      quadToRelative(-12f, 12f, -28f, 12f)
      reflectiveQuadToRelative(-28f, -12f)
      lineTo(183f, 441f)
      quadToRelative(-12f, -12f, -11.5f, -28.5f)
      reflectiveQuadTo(184f, 384f)
      quadToRelative(12f, -12f, 28.5f, -12f)
      reflectiveQuadToRelative(28.5f, 12f)
      lineToRelative(141f, 142f)
      close()
      moveTo(240f, 800f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(200f, 760f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(240f, 720f)
      horizontalLineToRelative(480f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(760f, 760f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(720f, 800f)
      lineTo(240f, 800f)
      close()
    }
  }.build()
}

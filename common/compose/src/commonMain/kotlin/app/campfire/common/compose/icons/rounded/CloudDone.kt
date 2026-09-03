// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.CloudDone: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "CloudDone",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveToRelative(413f, 565f)
      lineToRelative(-56f, -56f)
      quadToRelative(-12f, -12f, -28f, -12f)
      reflectiveQuadToRelative(-28f, 12f)
      quadToRelative(-12f, 12f, -12f, 28.5f)
      reflectiveQuadToRelative(12f, 28.5f)
      lineToRelative(85f, 86f)
      quadToRelative(12f, 12f, 28f, 12f)
      reflectiveQuadToRelative(28f, -12f)
      lineToRelative(169f, -169f)
      quadToRelative(12f, -12f, 12f, -29f)
      reflectiveQuadToRelative(-12f, -29f)
      quadToRelative(-12f, -12f, -29f, -12f)
      reflectiveQuadToRelative(-29f, 12f)
      lineTo(413f, 565f)
      close()
      moveTo(260f, 800f)
      quadToRelative(-91f, 0f, -155.5f, -63f)
      reflectiveQuadTo(40f, 583f)
      quadToRelative(0f, -78f, 47f, -139f)
      reflectiveQuadToRelative(123f, -78f)
      quadToRelative(25f, -92f, 100f, -149f)
      reflectiveQuadToRelative(170f, -57f)
      quadToRelative(117f, 0f, 198.5f, 81.5f)
      reflectiveQuadTo(760f, 440f)
      quadToRelative(69f, 8f, 114.5f, 59.5f)
      reflectiveQuadTo(920f, 620f)
      quadToRelative(0f, 75f, -52.5f, 127.5f)
      reflectiveQuadTo(740f, 800f)
      lineTo(260f, 800f)
      close()
      moveTo(260f, 720f)
      horizontalLineToRelative(480f)
      quadToRelative(42f, 0f, 71f, -29f)
      reflectiveQuadToRelative(29f, -71f)
      quadToRelative(0f, -42f, -29f, -71f)
      reflectiveQuadToRelative(-71f, -29f)
      horizontalLineToRelative(-60f)
      verticalLineToRelative(-80f)
      quadToRelative(0f, -83f, -58.5f, -141.5f)
      reflectiveQuadTo(480f, 240f)
      quadToRelative(-83f, 0f, -141.5f, 58.5f)
      reflectiveQuadTo(280f, 440f)
      horizontalLineToRelative(-20f)
      quadToRelative(-58f, 0f, -99f, 41f)
      reflectiveQuadToRelative(-41f, 99f)
      quadToRelative(0f, 58f, 41f, 99f)
      reflectiveQuadToRelative(99f, 41f)
      close()
      moveTo(480f, 480f)
      close()
    }
  }.build()
}

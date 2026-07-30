// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.filled

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Filled.BookRibbon: ImageVector by
  lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
      name = "Filled.BookRibbon",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f,
    ).apply {
      path(fill = SolidColor(Color(0xFFE8EAED))) {
        moveTo(40f, 726f)
        verticalLineToRelative(-482f)
        quadToRelative(0f, -11f, 5.5f, -21f)
        reflectiveQuadTo(62f, 208f)
        quadToRelative(46f, -24f, 96f, -36f)
        reflectiveQuadToRelative(102f, -12f)
        quadToRelative(74f, 0f, 126f, 17f)
        reflectiveQuadToRelative(112f, 52f)
        quadToRelative(11f, 6f, 16.5f, 14f)
        reflectiveQuadToRelative(5.5f, 21f)
        verticalLineToRelative(418f)
        quadToRelative(44f, -21f, 88.5f, -31.5f)
        reflectiveQuadTo(700f, 640f)
        quadToRelative(36f, 0f, 70.5f, 6f)
        reflectiveQuadToRelative(69.5f, 18f)
        verticalLineToRelative(-441f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(880f, 183f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(920f, 223f)
        verticalLineToRelative(503f)
        quadToRelative(0f, 23f, -19.5f, 35f)
        reflectiveQuadToRelative(-40.5f, 1f)
        quadToRelative(-37f, -20f, -77.5f, -31f)
        reflectiveQuadTo(700f, 720f)
        quadToRelative(-49f, 0f, -95.5f, 14.5f)
        reflectiveQuadTo(516f, 775f)
        quadToRelative(-8f, 5f, -17.5f, 7.5f)
        reflectiveQuadTo(480f, 785f)
        quadToRelative(-9f, 0f, -18.5f, -2.5f)
        reflectiveQuadTo(444f, 775f)
        quadToRelative(-42f, -26f, -88.5f, -40.5f)
        reflectiveQuadTo(260f, 720f)
        quadToRelative(-42f, 0f, -82.5f, 11f)
        reflectiveQuadTo(100f, 762f)
        quadToRelative(-21f, 11f, -40.5f, -1f)
        reflectiveQuadTo(40f, 726f)
        close()
        moveTo(620f, 518f)
        verticalLineToRelative(-369f)
        quadToRelative(0f, -13f, 7.5f, -23.5f)
        reflectiveQuadTo(647f, 111f)
        lineToRelative(54f, -18f)
        quadToRelative(14f, -5f, 26.5f, 4.5f)
        reflectiveQuadTo(740f, 122f)
        verticalLineToRelative(369f)
        quadToRelative(0f, 13f, -7.5f, 23.5f)
        reflectiveQuadTo(713f, 529f)
        lineToRelative(-54f, 18f)
        quadToRelative(-14f, 5f, -26.5f, -4.5f)
        reflectiveQuadTo(620f, 518f)
        close()
      }
    }.build()
  }

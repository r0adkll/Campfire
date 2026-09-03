// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.KeyboardDoubleArrowRight: ImageVector by
  lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
      name = "KeyboardDoubleArrowRight",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f,
    ).apply {
      path(fill = SolidColor(Color.Black)) {
        moveTo(383f, 480f)
        lineTo(228f, 324f)
        quadToRelative(-11f, -11f, -11.5f, -27.5f)
        reflectiveQuadTo(228f, 268f)
        quadToRelative(11f, -11f, 28f, -11f)
        reflectiveQuadToRelative(28f, 11f)
        lineToRelative(184f, 184f)
        quadToRelative(6f, 6f, 8.5f, 13f)
        reflectiveQuadToRelative(2.5f, 15f)
        quadToRelative(0f, 8f, -2.5f, 15f)
        reflectiveQuadToRelative(-8.5f, 13f)
        lineTo(284f, 692f)
        quadToRelative(-11f, 11f, -27.5f, 11.5f)
        reflectiveQuadTo(228f, 692f)
        quadToRelative(-11f, -11f, -11f, -28f)
        reflectiveQuadToRelative(11f, -28f)
        lineToRelative(155f, -156f)
        close()
        moveTo(647f, 480f)
        lineTo(492f, 324f)
        quadToRelative(-11f, -11f, -11.5f, -27.5f)
        reflectiveQuadTo(492f, 268f)
        quadToRelative(11f, -11f, 28f, -11f)
        reflectiveQuadToRelative(28f, 11f)
        lineToRelative(184f, 184f)
        quadToRelative(6f, 6f, 8.5f, 13f)
        reflectiveQuadToRelative(2.5f, 15f)
        quadToRelative(0f, 8f, -2.5f, 15f)
        reflectiveQuadToRelative(-8.5f, 13f)
        lineTo(548f, 692f)
        quadToRelative(-11f, 11f, -27.5f, 11.5f)
        reflectiveQuadTo(492f, 692f)
        quadToRelative(-11f, -11f, -11f, -28f)
        reflectiveQuadToRelative(11f, -28f)
        lineToRelative(155f, -156f)
        close()
      }
    }.build()
  }

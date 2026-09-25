// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.Wifi: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "Wifi",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(409f, 811f)
      quadToRelative(-29f, -29f, -29f, -71f)
      reflectiveQuadToRelative(29f, -71f)
      quadToRelative(29f, -29f, 71f, -29f)
      reflectiveQuadToRelative(71f, 29f)
      quadToRelative(29f, 29f, 29f, 71f)
      reflectiveQuadToRelative(-29f, 71f)
      quadToRelative(-29f, 29f, -71f, 29f)
      reflectiveQuadToRelative(-71f, -29f)
      close()
      moveTo(622.5f, 424f)
      quadTo(690f, 448f, 745f, 490f)
      quadToRelative(20f, 15f, 20.5f, 39.5f)
      reflectiveQuadTo(748f, 572f)
      quadToRelative(-17f, 17f, -42f, 17.5f)
      reflectiveQuadTo(661f, 576f)
      quadToRelative(-38f, -26f, -84f, -41f)
      reflectiveQuadToRelative(-97f, -15f)
      quadToRelative(-51f, 0f, -97f, 15f)
      reflectiveQuadToRelative(-84f, 41f)
      quadToRelative(-20f, 14f, -45f, 13f)
      reflectiveQuadToRelative(-42f, -18f)
      quadToRelative(-17f, -18f, -17f, -42.5f)
      reflectiveQuadToRelative(20f, -39.5f)
      quadToRelative(55f, -42f, 122.5f, -65.5f)
      reflectiveQuadTo(480f, 400f)
      quadToRelative(75f, 0f, 142.5f, 24f)
      close()
      moveTo(715.5f, 201f)
      quadTo(826f, 242f, 914f, 317f)
      quadToRelative(20f, 17f, 21f, 42f)
      reflectiveQuadToRelative(-17f, 43f)
      quadToRelative(-17f, 17f, -42f, 17.5f)
      reflectiveQuadTo(831f, 404f)
      quadToRelative(-72f, -59f, -161.5f, -91.5f)
      reflectiveQuadTo(480f, 280f)
      quadToRelative(-100f, 0f, -189.5f, 32.5f)
      reflectiveQuadTo(129f, 404f)
      quadToRelative(-20f, 16f, -45f, 15.5f)
      reflectiveQuadTo(42f, 402f)
      quadToRelative(-18f, -18f, -17f, -43f)
      reflectiveQuadToRelative(21f, -42f)
      quadToRelative(88f, -75f, 198.5f, -116f)
      reflectiveQuadTo(480f, 160f)
      quadToRelative(125f, 0f, 235.5f, 41f)
      close()
    }
  }.build()
}

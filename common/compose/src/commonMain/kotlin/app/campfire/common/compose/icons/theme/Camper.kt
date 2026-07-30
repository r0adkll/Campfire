// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Theme.Camper: ImageVector by lazy(LazyThreadSafetyMode.PUBLICATION) {
  ImageVector.Builder(
    name = "Theme.Camper",
    defaultWidth = 64.dp,
    defaultHeight = 64.dp,
    viewportWidth = 64f,
    viewportHeight = 64f,
  ).apply {
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(13f, 61f)
      arcToRelative(19f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, 38f, 0f)
      arcToRelative(19f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, -38f, 0f)
      close()
    }
    path(fill = SolidColor(Color(0xFF68E5FD))) {
      moveTo(36f, 46f)
      horizontalLineTo(10f)
      curveToRelative(-2.209f, 0f, -4f, -1.791f, -4f, -4f)
      verticalLineTo(14f)
      curveToRelative(0f, -2.209f, 1.791f, -4f, 4f, -4f)
      horizontalLineToRelative(39f)
      curveToRelative(3.314f, 0f, 6f, 2.686f, 6f, 6f)
      verticalLineToRelative(0f)
      curveToRelative(0f, 1.657f, -1.343f, 3f, -3f, 3f)
      lineToRelative(-9f, 0f)
      curveToRelative(-2.761f, 0f, -5f, 2.239f, -5f, 5f)
      verticalLineToRelative(20f)
      curveTo(38f, 45.105f, 37.105f, 46f, 36f, 46f)
      close()
    }
    path(fill = SolidColor(Color(0xFF37D0EE))) {
      moveTo(36f, 46f)
      horizontalLineTo(10f)
      curveToRelative(-2.209f, 0f, -4f, -1.791f, -4f, -4f)
      verticalLineToRelative(-4f)
      horizontalLineToRelative(32f)
      verticalLineToRelative(6f)
      curveTo(38f, 45.105f, 37.105f, 46f, 36f, 46f)
      close()
    }
    path(fill = SolidColor(Color(0xFF9C34C2))) {
      moveTo(40f, 46f)
      horizontalLineToRelative(16f)
      curveToRelative(1.105f, 0f, 2f, -0.895f, 2f, -2f)
      verticalLineTo(33.114f)
      curveToRelative(0f, -0.736f, -0.136f, -1.467f, -0.4f, -2.154f)
      lineToRelative(-3.86f, -10.037f)
      curveTo(53.294f, 19.764f, 52.181f, 19f, 50.94f, 19f)
      horizontalLineTo(40f)
      curveToRelative(-1.105f, 0f, -2f, 0.895f, -2f, 2f)
      verticalLineToRelative(23f)
      curveTo(38f, 45.105f, 38.895f, 46f, 40f, 46f)
      close()
    }
    path(fill = SolidColor(Color(0xFF008AA9))) {
      moveTo(18f, 46f)
      moveToRelative(-6f, 0f)
      arcToRelative(6f, 6f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 0f)
      arcToRelative(6f, 6f, 0f, isMoreThanHalf = true, isPositiveArc = true, -12f, 0f)
    }
    path(fill = SolidColor(Color(0xFF37D0EE))) {
      moveTo(18f, 46f)
      moveToRelative(-3f, 0f)
      arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, 6f, 0f)
      arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, -6f, 0f)
    }
    path(fill = SolidColor(Color(0xFF008AA9))) {
      moveTo(22f, 19f)
      horizontalLineToRelative(11f)
      curveToRelative(1.105f, 0f, 2f, 0.895f, 2f, 2f)
      verticalLineToRelative(7f)
      curveToRelative(0f, 1.105f, -0.895f, 2f, -2f, 2f)
      horizontalLineTo(22f)
      curveToRelative(-1.105f, 0f, -2f, -0.895f, -2f, -2f)
      verticalLineToRelative(-7f)
      curveTo(20f, 19.895f, 20.895f, 19f, 22f, 19f)
      close()
    }
    path(fill = SolidColor(Color(0xFF008AA9))) {
      moveTo(13f, 19f)
      horizontalLineToRelative(2f)
      curveToRelative(1.105f, 0f, 2f, 0.895f, 2f, 2f)
      verticalLineToRelative(3f)
      curveToRelative(0f, 1.105f, -0.895f, 2f, -2f, 2f)
      horizontalLineToRelative(-2f)
      curveToRelative(-1.105f, 0f, -2f, -0.895f, -2f, -2f)
      verticalLineToRelative(-3f)
      curveTo(11f, 19.895f, 11.895f, 19f, 13f, 19f)
      close()
    }
    path(fill = SolidColor(Color(0xFF008AA9))) {
      moveTo(39.5f, 13f)
      horizontalLineToRelative(9f)
      curveToRelative(0.828f, 0f, 1.5f, 0.672f, 1.5f, 1.5f)
      verticalLineToRelative(0f)
      curveToRelative(0f, 0.828f, -0.672f, 1.5f, -1.5f, 1.5f)
      lineToRelative(-9f, 0f)
      curveToRelative(-0.828f, 0f, -1.5f, -0.672f, -1.5f, -1.5f)
      verticalLineToRelative(0f)
      curveTo(38f, 13.672f, 38.672f, 13f, 39.5f, 13f)
      close()
    }
    path(fill = SolidColor(Color(0xFF37D0EE))) {
      moveTo(44f, 32f)
      horizontalLineToRelative(6.663f)
      curveToRelative(1.622f, 0f, 2.751f, -1.612f, 2.196f, -3.136f)
      lineToRelative(-2.381f, -5.548f)
      curveTo(50.191f, 22.526f, 49.44f, 22f, 48.599f, 22f)
      horizontalLineTo(44f)
      curveToRelative(-1.105f, 0f, -2f, 0.895f, -2f, 2f)
      verticalLineToRelative(6f)
      curveTo(42f, 31.105f, 42.895f, 32f, 44f, 32f)
      close()
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(58f, 33.11f)
      verticalLineTo(44f)
      curveToRelative(0f, 1.1f, -0.9f, 2f, -2f, 2f)
      horizontalLineTo(43f)
      curveToRelative(0f, -2.42f, 1.72f, -4.44f, 4f, -4.9f)
      lineToRelative(5.01f, -0.09f)
      curveToRelative(0.55f, -0.01f, 0.99f, -0.45f, 0.99f, -1f)
      verticalLineTo(37f)
      curveToRelative(0f, -2.73f, 2.19f, -4.95f, 4.9f, -4.99f)
      curveTo(57.96f, 32.37f, 58f, 32.74f, 58f, 33.11f)
      close()
    }
    path(
      fill = SolidColor(Color.White),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(27f, 10f)
      curveToRelative(0f, 2.76f, -2.24f, 5f, -5f, 5f)
      horizontalLineTo(11f)
      verticalLineToRelative(11f)
      curveToRelative(0f, 2.761f, -2.239f, 5f, -5f, 5f)
      horizontalLineToRelative(0f)
      verticalLineTo(14f)
      curveToRelative(0f, -2.209f, 1.791f, -4f, 4f, -4f)
      horizontalLineTo(27f)
      close()
    }
    path(fill = SolidColor(Color(0xFF008AA9))) {
      moveTo(48f, 46f)
      moveToRelative(-6f, 0f)
      arcToRelative(6f, 6f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 0f)
      arcToRelative(6f, 6f, 0f, isMoreThanHalf = true, isPositiveArc = true, -12f, 0f)
    }
    path(fill = SolidColor(Color(0xFF37D0EE))) {
      moveTo(48f, 46f)
      moveToRelative(-3f, 0f)
      arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, 6f, 0f)
      arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, -6f, 0f)
    }
    path(fill = SolidColor(Color.White)) {
      moveTo(9.5f, 20f)
      curveTo(8.672f, 20f, 8f, 19.329f, 8f, 18.5f)
      verticalLineToRelative(-3f)
      curveToRelative(0f, -1.93f, 1.57f, -3.5f, 3.5f, -3.5f)
      horizontalLineToRelative(3f)
      curveToRelative(0.828f, 0f, 1.5f, 0.671f, 1.5f, 1.5f)
      reflectiveCurveTo(15.328f, 15f, 14.5f, 15f)
      horizontalLineToRelative(-3f)
      curveToRelative(-0.275f, 0f, -0.5f, 0.224f, -0.5f, 0.5f)
      verticalLineToRelative(3f)
      curveTo(11f, 19.329f, 10.328f, 20f, 9.5f, 20f)
      close()
    }
  }.build()
}

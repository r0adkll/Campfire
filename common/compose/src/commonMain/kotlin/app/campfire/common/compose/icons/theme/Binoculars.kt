// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Theme.Binoculars: ImageVector by lazy(LazyThreadSafetyMode.PUBLICATION) {
  ImageVector.Builder(
    name = "Theme.Binoculars",
    defaultWidth = 64.dp,
    defaultHeight = 64.dp,
    viewportWidth = 64f,
    viewportHeight = 64f,
  ).apply {
    path(fill = SolidColor(Color(0xFF9C34C2))) {
      moveTo(58f, 39f)
      horizontalLineTo(6f)
      verticalLineToRelative(0f)
      curveToRelative(0f, -2.642f, 0.504f, -5.26f, 1.486f, -7.714f)
      lineToRelative(3.956f, -9.887f)
      curveToRelative(1.063f, -2.657f, 3.637f, -4.4f, 6.499f, -4.4f)
      horizontalLineToRelative(28.118f)
      curveToRelative(2.862f, 0f, 5.436f, 1.742f, 6.499f, 4.4f)
      lineToRelative(3.956f, 9.887f)
      curveTo(57.496f, 33.74f, 58f, 36.358f, 58f, 39f)
      lineTo(58f, 39f)
      close()
    }
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
    path(fill = SolidColor(Color(0xFF9C34C2))) {
      moveTo(18f, 39f)
      moveToRelative(-12f, 0f)
      arcToRelative(12f, 12f, 0f, isMoreThanHalf = true, isPositiveArc = true, 24f, 0f)
      arcToRelative(12f, 12f, 0f, isMoreThanHalf = true, isPositiveArc = true, -24f, 0f)
    }
    path(fill = SolidColor(Color(0xFF4CCFF1))) {
      moveTo(18f, 39f)
      moveToRelative(-7f, 0f)
      arcToRelative(7f, 7f, 0f, isMoreThanHalf = true, isPositiveArc = true, 14f, 0f)
      arcToRelative(7f, 7f, 0f, isMoreThanHalf = true, isPositiveArc = true, -14f, 0f)
    }
    path(fill = SolidColor(Color.White)) {
      moveTo(14.5f, 40.5f)
      curveToRelative(-0.828f, 0f, -1.5f, -0.672f, -1.5f, -1.5f)
      curveToRelative(0f, -2.757f, 2.243f, -5f, 5f, -5f)
      curveToRelative(0.828f, 0f, 1.5f, 0.672f, 1.5f, 1.5f)
      reflectiveCurveTo(18.828f, 37f, 18f, 37f)
      curveToRelative(-1.103f, 0f, -2f, 0.897f, -2f, 2f)
      curveTo(16f, 39.828f, 15.328f, 40.5f, 14.5f, 40.5f)
      close()
    }
    path(fill = SolidColor(Color(0xFF9C34C2))) {
      moveTo(23f, 19f)
      moveToRelative(-6f, 0f)
      arcToRelative(6f, 6f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 0f)
      arcToRelative(6f, 6f, 0f, isMoreThanHalf = true, isPositiveArc = true, -12f, 0f)
    }
    path(fill = SolidColor(Color(0xFF9C34C2))) {
      moveTo(41f, 19f)
      moveToRelative(-6f, 0f)
      arcToRelative(6f, 6f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 0f)
      arcToRelative(6f, 6f, 0f, isMoreThanHalf = true, isPositiveArc = true, -12f, 0f)
    }
    path(fill = SolidColor(Color(0xFF9C34C2))) {
      moveTo(46f, 39f)
      moveToRelative(-12f, 0f)
      arcToRelative(12f, 12f, 0f, isMoreThanHalf = true, isPositiveArc = true, 24f, 0f)
      arcToRelative(12f, 12f, 0f, isMoreThanHalf = true, isPositiveArc = true, -24f, 0f)
    }
    path(fill = SolidColor(Color(0xFF4CCFF1))) {
      moveTo(46f, 39f)
      moveToRelative(-7f, 0f)
      arcToRelative(7f, 7f, 0f, isMoreThanHalf = true, isPositiveArc = true, 14f, 0f)
      arcToRelative(7f, 7f, 0f, isMoreThanHalf = true, isPositiveArc = true, -14f, 0f)
    }
    path(fill = SolidColor(Color(0xFF9C34C2))) {
      moveTo(32f, 38.5f)
      moveToRelative(-5.5f, 0f)
      arcToRelative(5.5f, 5.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 11f, 0f)
      arcToRelative(5.5f, 5.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, -11f, 0f)
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(52.558f, 21.4f)
      curveToRelative(-0.654f, -1.635f, -1.889f, -2.903f, -3.404f, -3.647f)
      curveToRelative(-1.463f, 1.367f, -2.026f, 3.533f, -1.238f, 5.504f)
      lineToRelative(3.956f, 9.888f)
      curveTo(52.62f, 35.014f, 53f, 36.983f, 53f, 39f)
      curveToRelative(0f, 3.859f, -3.141f, 7f, -7f, 7f)
      curveToRelative(-2.401f, 0f, -4.403f, 1.694f, -4.886f, 3.951f)
      curveTo(42.607f, 50.62f, 44.257f, 51f, 46f, 51f)
      curveToRelative(6.627f, 0f, 12f, -5.373f, 12f, -12f)
      curveToRelative(0f, -2.642f, -0.504f, -5.26f, -1.486f, -7.714f)
      lineTo(52.558f, 21.4f)
      close()
    }
    path(
      fill = SolidColor(Color.White),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(27.526f, 15.095f)
      curveTo(26.428f, 13.819f, 24.816f, 13f, 23f, 13f)
      curveToRelative(-2.629f, 0f, -4.857f, 1.693f, -5.668f, 4.046f)
      curveToRelative(-2.612f, 0.229f, -4.903f, 1.888f, -5.89f, 4.353f)
      lineToRelative(-3.956f, 9.887f)
      curveToRelative(-0.62f, 1.551f, -1.027f, 3.171f, -1.259f, 4.817f)
      curveToRelative(0.419f, 0.109f, 0.839f, 0.184f, 1.258f, 0.184f)
      curveToRelative(1.984f, 0f, 3.862f, -1.189f, 4.645f, -3.144f)
      lineToRelative(3.954f, -9.886f)
      curveToRelative(0.277f, -0.692f, 0.924f, -1.164f, 1.687f, -1.231f)
      curveToRelative(1.966f, -0.173f, 3.646f, -1.484f, 4.289f, -3.35f)
      curveTo(22.175f, 18.34f, 22.511f, 18f, 23f, 18f)
      curveTo(25.011f, 18f, 26.733f, 16.806f, 27.526f, 15.095f)
      close()
    }
    path(fill = SolidColor(Color(0xFFFFA602))) {
      moveTo(32f, 38f)
      moveToRelative(-3f, 0f)
      arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, 6f, 0f)
      arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, -6f, 0f)
    }
    path(fill = SolidColor(Color.White)) {
      moveTo(42.5f, 40.5f)
      curveToRelative(-0.828f, 0f, -1.5f, -0.672f, -1.5f, -1.5f)
      curveToRelative(0f, -2.757f, 2.243f, -5f, 5f, -5f)
      curveToRelative(0.828f, 0f, 1.5f, 0.672f, 1.5f, 1.5f)
      reflectiveCurveTo(46.828f, 37f, 46f, 37f)
      curveToRelative(-1.103f, 0f, -2f, 0.897f, -2f, 2f)
      curveTo(44f, 39.828f, 43.328f, 40.5f, 42.5f, 40.5f)
      close()
    }
    path(
      stroke = SolidColor(Color.White),
      strokeLineWidth = 3f,
      strokeLineCap = StrokeCap.Round,
    ) {
      moveTo(14.921f, 22.303f)
      lineTo(13.152f, 26.5f)
    }
  }.build()
}

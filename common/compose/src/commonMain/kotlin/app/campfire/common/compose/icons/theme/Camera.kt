// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Theme.Camera: ImageVector by lazy(LazyThreadSafetyMode.PUBLICATION) {
  ImageVector.Builder(
    name = "Theme.Camera",
    defaultWidth = 64.dp,
    defaultHeight = 64.dp,
    viewportWidth = 64f,
    viewportHeight = 64f,
  ).apply {
    path(fill = SolidColor(Color(0xFF9C34C2))) {
      moveTo(20f, 20f)
      horizontalLineToRelative(-7f)
      verticalLineToRelative(-3f)
      curveToRelative(0f, -1.105f, 0.895f, -2f, 2f, -2f)
      horizontalLineToRelative(3f)
      curveToRelative(1.105f, 0f, 2f, 0.895f, 2f, 2f)
      verticalLineTo(20f)
      close()
    }
    path(fill = SolidColor(Color(0xFFFFA500))) {
      moveTo(51f, 18f)
      horizontalLineToRelative(-6.141f)
      curveToRelative(-1.231f, 0f, -2.393f, -0.567f, -3.151f, -1.536f)
      lineToRelative(-1.507f, -1.927f)
      curveTo(39.443f, 13.567f, 38.281f, 13f, 37.05f, 13f)
      horizontalLineToRelative(-10.1f)
      curveToRelative(-1.231f, 0f, -2.393f, 0.567f, -3.151f, 1.536f)
      lineToRelative(-1.507f, 1.927f)
      curveTo(21.534f, 17.433f, 20.372f, 18f, 19.141f, 18f)
      horizontalLineTo(13f)
      curveToRelative(-3.314f, 0f, -6f, 2.686f, -6f, 6f)
      verticalLineToRelative(22f)
      curveToRelative(0f, 3.314f, 2.686f, 6f, 6f, 6f)
      horizontalLineToRelative(38f)
      curveToRelative(3.314f, 0f, 6f, -2.686f, 6f, -6f)
      verticalLineTo(24f)
      curveTo(57f, 20.686f, 54.314f, 18f, 51f, 18f)
      close()
    }
    path(fill = SolidColor(Color(0xFF9C34C2))) {
      moveTo(48.5f, 25.5f)
      moveToRelative(-2.5f, 0f)
      arcToRelative(2.5f, 2.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 5f, 0f)
      arcToRelative(2.5f, 2.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, -5f, 0f)
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(57f, 46f)
      verticalLineTo(29f)
      curveToRelative(-2.761f, 0f, -5f, 2.238f, -5f, 5f)
      verticalLineToRelative(12f)
      curveToRelative(0f, 0.552f, -0.449f, 1f, -1f, 1f)
      horizontalLineToRelative(-7f)
      curveToRelative(-2.761f, 0f, -5f, 2.238f, -5f, 5f)
      horizontalLineToRelative(12f)
      curveTo(54.314f, 52f, 57f, 49.314f, 57f, 46f)
      close()
    }
    path(
      fill = SolidColor(Color.White),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(23.799f, 14.536f)
      lineToRelative(-1.507f, 1.927f)
      curveTo(21.534f, 17.433f, 20.372f, 18f, 19.141f, 18f)
      horizontalLineTo(13f)
      curveToRelative(-3.314f, 0f, -6f, 2.686f, -6f, 6f)
      verticalLineToRelative(13.213f)
      curveToRelative(2.762f, 0f, 5f, -2.239f, 5f, -5f)
      verticalLineToRelative(-8.958f)
      curveTo(12f, 23.114f, 12.114f, 23f, 12.255f, 23f)
      horizontalLineToRelative(6.886f)
      curveToRelative(2.77f, 0f, 5.386f, -1.275f, 7.091f, -3.458f)
      lineToRelative(0.605f, -0.774f)
      curveTo(27.216f, 18.283f, 27.797f, 18f, 28.413f, 18f)
      horizontalLineToRelative(1.34f)
      curveToRelative(2.762f, 0f, 5f, -2.239f, 5f, -5f)
      horizontalLineTo(26.95f)
      curveTo(25.719f, 13f, 24.557f, 13.567f, 23.799f, 14.536f)
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
      moveTo(32f, 34.5f)
      moveToRelative(-12.5f, 0f)
      arcToRelative(12.5f, 12.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 25f, 0f)
      arcToRelative(12.5f, 12.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, -25f, 0f)
    }
    path(fill = SolidColor(Color(0xFF4CCFF1))) {
      moveTo(32f, 34.5f)
      moveToRelative(-8.5f, 0f)
      arcToRelative(8.5f, 8.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 17f, 0f)
      arcToRelative(8.5f, 8.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, -17f, 0f)
    }
    path(
      stroke = SolidColor(Color.White),
      strokeLineWidth = 3f,
      strokeLineCap = StrokeCap.Round,
      strokeLineJoin = StrokeJoin.Round,
    ) {
      moveTo(10.514f, 27.045f)
      verticalLineToRelative(-3.5f)
      curveToRelative(0f, -1.103f, 0.897f, -2f, 2f, -2f)
      horizontalLineToRelative(3.5f)
    }
  }.build()
}

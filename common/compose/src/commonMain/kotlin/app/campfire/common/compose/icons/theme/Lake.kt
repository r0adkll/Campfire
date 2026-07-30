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

val CampfireIcons.Theme.Lake: ImageVector by lazy(LazyThreadSafetyMode.PUBLICATION) {
  ImageVector.Builder(
    name = "Theme.Lake",
    defaultWidth = 64.dp,
    defaultHeight = 64.dp,
    viewportWidth = 64f,
    viewportHeight = 64f,
  ).apply {
    path(fill = SolidColor(Color(0xFFBD6300))) {
      moveTo(42f, 31f)
      lineTo(42f, 31f)
      curveToRelative(-1.105f, 0f, -2f, -0.895f, -2f, -2f)
      verticalLineToRelative(-6f)
      horizontalLineToRelative(4f)
      verticalLineToRelative(6f)
      curveTo(44f, 30.105f, 43.105f, 31f, 42f, 31f)
      close()
    }
    path(fill = SolidColor(Color(0xFFBD6300))) {
      moveTo(50f, 31f)
      lineTo(50f, 31f)
      curveToRelative(-1.105f, 0f, -2f, -0.895f, -2f, -2f)
      verticalLineToRelative(-6f)
      horizontalLineToRelative(4f)
      verticalLineToRelative(6f)
      curveTo(52f, 30.105f, 51.105f, 31f, 50f, 31f)
      close()
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(9f, 61f)
      arcToRelative(23f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, 46f, 0f)
      arcToRelative(23f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, -46f, 0f)
      close()
    }
    path(fill = SolidColor(Color(0xFF37D0EE))) {
      moveTo(48.635f, 33f)
      horizontalLineTo(37.484f)
      curveToRelative(-2.667f, 0f, -4.7f, -2.286f, -4.505f, -4.946f)
      curveToRelative(0.022f, -0.299f, 0.026f, -0.603f, 0.012f, -0.91f)
      curveTo(32.803f, 23.09f, 29.252f, 20f, 25.194f, 20f)
      curveToRelative(0f, 0f, -5.545f, 0.006f, -6.667f, 0.271f)
      curveTo(10.281f, 21.685f, 4f, 28.851f, 4f, 37.5f)
      curveTo(4f, 47.165f, 11.835f, 55f, 21.5f, 55f)
      curveToRelative(0.443f, 0f, 27.5f, 0f, 27.5f, 0f)
      curveToRelative(6.144f, 0f, 11.111f, -5.037f, 10.998f, -11.206f)
      curveTo(59.887f, 37.738f, 54.692f, 33f, 48.635f, 33f)
      close()
    }
    path(
      stroke = SolidColor(Color(0xFFA0EFFE)),
      strokeLineWidth = 3f,
      strokeLineCap = StrokeCap.Round,
      strokeLineJoin = StrokeJoin.Round,
    ) {
      moveTo(30.5f, 47f)
      curveToRelative(-5.238f, 0f, -12f, -2.5f, -14f, -9.5f)
    }
    path(
      stroke = SolidColor(Color(0xFFA0EFFE)),
      strokeLineWidth = 3f,
      strokeLineCap = StrokeCap.Round,
      strokeLineJoin = StrokeJoin.Round,
    ) {
      moveTo(24.974f, 28f)
      curveToRelative(-0.114f, 3.298f, 1.098f, 6.574f, 3.366f, 9.016f)
      curveTo(30.693f, 39.548f, 34.026f, 41f, 37.484f, 41f)
    }
    path(
      stroke = SolidColor(Color.White),
      strokeLineWidth = 3f,
      strokeLineCap = StrokeCap.Round,
      strokeLineJoin = StrokeJoin.Round,
    ) {
      moveTo(13.281f, 26.166f)
      curveToRelative(1.686f, -1.217f, 3.665f, -2.073f, 5.837f, -2.445f)
      lineToRelative(0.107f, -0.019f)
    }
    path(
      fill = SolidColor(Color.White),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(23.496f, 20.014f)
      curveToRelative(-1.728f, 0.024f, -4.251f, 0.087f, -4.969f, 0.257f)
      curveTo(10.281f, 21.685f, 4f, 28.851f, 4f, 37.5f)
      curveToRelative(0f, 1.713f, 0.257f, 3.363f, 0.716f, 4.928f)
      curveTo(7.135f, 42.078f, 9f, 40.016f, 9f, 37.5f)
      curveToRelative(0f, -6.097f, 4.362f, -11.271f, 10.372f, -12.301f)
      curveTo(21.896f, 24.766f, 23.626f, 22.51f, 23.496f, 20.014f)
      close()
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(59.998f, 43.794f)
      curveToRelative(-0.032f, -1.746f, -0.497f, -3.376f, -1.273f, -4.821f)
      curveToRelative(-2.177f, 0.574f, -3.769f, 2.562f, -3.726f, 4.912f)
      curveToRelative(0.03f, 1.626f, -0.58f, 3.16f, -1.717f, 4.318f)
      curveTo(52.144f, 49.362f, 50.624f, 50f, 49f, 50f)
      horizontalLineToRelative(-9f)
      curveToRelative(-2.761f, 0f, -5f, 2.239f, -5f, 5f)
      curveToRelative(6.958f, 0f, 14f, 0f, 14f, 0f)
      curveTo(55.144f, 55f, 60.111f, 49.963f, 59.998f, 43.794f)
      close()
    }
    path(fill = SolidColor(Color(0xFF548500))) {
      moveTo(47.033f, 18.194f)
      curveTo(47.624f, 17.646f, 48f, 16.87f, 48f, 16f)
      curveToRelative(0f, -1.657f, -1.343f, -3f, -3f, -3f)
      curveToRelative(-0.065f, 0f, -0.126f, 0.015f, -0.19f, 0.019f)
      curveTo(44.926f, 12.7f, 45f, 12.36f, 45f, 12f)
      curveToRelative(0f, -1.657f, -1.343f, -3f, -3f, -3f)
      reflectiveCurveToRelative(-3f, 1.343f, -3f, 3f)
      curveToRelative(0f, 0.36f, 0.074f, 0.7f, 0.19f, 1.019f)
      curveTo(39.126f, 13.015f, 39.065f, 13f, 39f, 13f)
      curveToRelative(-1.657f, 0f, -3f, 1.343f, -3f, 3f)
      curveToRelative(0f, 0.87f, 0.376f, 1.646f, 0.967f, 2.194f)
      curveTo(35.821f, 18.616f, 35f, 19.708f, 35f, 21f)
      curveToRelative(0f, 1.657f, 1.343f, 3f, 3f, 3f)
      horizontalLineToRelative(8f)
      curveToRelative(1.657f, 0f, 3f, -1.343f, 3f, -3f)
      curveTo(49f, 19.708f, 48.179f, 18.616f, 47.033f, 18.194f)
      close()
    }
    path(fill = SolidColor(Color(0xFF98C900))) {
      moveTo(56.912f, 23.208f)
      lineTo(53.895f, 19f)
      horizontalLineToRelative(0.609f)
      curveToRelative(0.186f, 0f, 0.356f, -0.103f, 0.442f, -0.267f)
      reflectiveCurveToRelative(0.075f, -0.363f, -0.03f, -0.516f)
      lineTo(52.708f, 15f)
      horizontalLineToRelative(0.798f)
      curveToRelative(0.186f, 0f, 0.357f, -0.104f, 0.443f, -0.269f)
      curveToRelative(0.086f, -0.165f, 0.073f, -0.364f, -0.033f, -0.517f)
      lineToRelative(-3.476f, -5f)
      curveTo(50.347f, 9.08f, 50.221f, 9.036f, 50.03f, 9f)
      curveToRelative(-0.162f, 0f, -0.314f, 0.079f, -0.408f, 0.211f)
      lineToRelative(-3.536f, 5f)
      curveToRelative(-0.108f, 0.152f, -0.122f, 0.353f, -0.036f, 0.519f)
      reflectiveCurveTo(46.307f, 15f, 46.494f, 15f)
      horizontalLineToRelative(0.845f)
      lineToRelative(-2.243f, 3.214f)
      curveToRelative(-0.106f, 0.153f, -0.119f, 0.352f, -0.033f, 0.518f)
      curveTo(45.149f, 18.896f, 45.32f, 19f, 45.506f, 19f)
      horizontalLineToRelative(0.65f)
      lineToRelative(-3.066f, 4.206f)
      curveToRelative(-0.111f, 0.152f, -0.127f, 0.354f, -0.042f, 0.521f)
      curveTo(43.134f, 23.895f, 43.306f, 24f, 43.494f, 24f)
      horizontalLineToRelative(13.012f)
      curveToRelative(0.188f, 0f, 0.359f, -0.105f, 0.445f, -0.271f)
      curveTo(57.036f, 23.562f, 57.021f, 23.361f, 56.912f, 23.208f)
      close()
    }
  }.build()
}

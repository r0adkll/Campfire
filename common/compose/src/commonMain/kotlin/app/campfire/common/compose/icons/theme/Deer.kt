// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Theme.Deer: ImageVector by lazy(LazyThreadSafetyMode.PUBLICATION) {
  ImageVector.Builder(
    name = "Theme.Deer",
    defaultWidth = 64.dp,
    defaultHeight = 64.dp,
    viewportWidth = 64f,
    viewportHeight = 64f,
  ).apply {
    path(fill = SolidColor(Color(0xFFBD6300))) {
      moveTo(15f, 20f)
      horizontalLineTo(9f)
      curveToRelative(-1.105f, 0f, -2f, 0.895f, -2f, 2f)
      verticalLineToRelative(0f)
      curveToRelative(0f, 3.866f, 3.134f, 7f, 7f, 7f)
      horizontalLineToRelative(8f)
      verticalLineToRelative(-2f)
      curveTo(22f, 23.134f, 18.866f, 20f, 15f, 20f)
      close()
    }
    path(fill = SolidColor(Color(0xFFBD6300))) {
      moveTo(49f, 20f)
      horizontalLineToRelative(6f)
      curveToRelative(1.105f, 0f, 2f, 0.895f, 2f, 2f)
      verticalLineToRelative(0f)
      curveToRelative(0f, 3.866f, -3.134f, 7f, -7f, 7f)
      horizontalLineToRelative(-8f)
      verticalLineToRelative(-2f)
      curveTo(42f, 23.134f, 45.134f, 20f, 49f, 20f)
      close()
    }
    path(fill = SolidColor(Color(0xFFA7B3C7))) {
      moveTo(32f, 22f)
      curveTo(12.164f, 22f, 8f, 12.212f, 8f, 4f)
      curveToRelative(0f, -1.657f, 1.343f, -3f, 3f, -3f)
      reflectiveCurveToRelative(3f, 1.343f, 3f, 3f)
      curveToRelative(0f, 5.151f, 1.866f, 12f, 18f, 12f)
      reflectiveCurveTo(50f, 9.151f, 50f, 4f)
      curveToRelative(0f, -1.657f, 1.343f, -3f, 3f, -3f)
      reflectiveCurveToRelative(3f, 1.343f, 3f, 3f)
      curveTo(56f, 12.212f, 51.836f, 22f, 32f, 22f)
      close()
    }
    path(fill = SolidColor(Color(0xFFA7B3C7))) {
      moveTo(32f, 20f)
      curveToRelative(-7.72f, 0f, -14f, -6.28f, -14f, -14f)
      curveToRelative(0f, -1.381f, 1.119f, -2.5f, 2.5f, -2.5f)
      reflectiveCurveTo(23f, 4.619f, 23f, 6f)
      curveToRelative(0f, 4.963f, 4.038f, 9f, 9f, 9f)
      reflectiveCurveToRelative(9f, -4.037f, 9f, -9f)
      curveToRelative(0f, -1.381f, 1.119f, -2.5f, 2.5f, -2.5f)
      reflectiveCurveTo(46f, 4.619f, 46f, 6f)
      curveTo(46f, 13.72f, 39.72f, 20f, 32f, 20f)
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
    path(fill = SolidColor(Color(0xFFFFA500))) {
      moveTo(47f, 28f)
      curveToRelative(0f, -9.695f, -8f, -13f, -15f, -12.976f)
      curveToRelative(-7.941f, 0.028f, -14.252f, 3.061f, -14.933f, 11.351f)
      curveToRelative(-0.433f, 5.267f, 1.264f, 10.083f, 4.22f, 13.499f)
      curveToRelative(1.177f, 1.361f, 1.89f, 1.149f, 1.89f, 3.054f)
      verticalLineToRelative(0f)
      curveToRelative(0f, 1.395f, 0.151f, 2.773f, 0.442f, 4.105f)
      curveToRelative(0.817f, 3.738f, 4.035f, 7.469f, 7.411f, 7.903f)
      curveToRelative(3.884f, 0.5f, 7.55f, -2.047f, 8.871f, -6.125f)
      curveToRelative(0.605f, -1.867f, 0.923f, -3.857f, 0.923f, -5.884f)
      verticalLineToRelative(-0.003f)
      curveToRelative(0f, -1.896f, 0.702f, -1.682f, 1.875f, -3.034f)
      curveTo(45.356f, 36.829f, 47f, 32.636f, 47f, 28f)
      close()
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(42.661f, 39.933f)
      curveToRelative(-0.466f, -0.193f, -0.958f, -0.34f, -1.488f, -0.384f)
      curveToRelative(-2.744f, -0.217f, -5.168f, 1.816f, -5.398f, 4.567f)
      curveToRelative(-0.09f, 1.08f, -2.237f, 2.067f, -2.565f, 3.081f)
      curveToRelative(-0.171f, 0.526f, -0.516f, 0.995f, -0.814f, 1.388f)
      curveToRelative(-0.71f, 0.938f, 0.314f, 1.525f, -0.728f, 1.393f)
      curveToRelative(-2.096f, -0.268f, -4.046f, 0.805f, -5.008f, 2.548f)
      curveToRelative(1.274f, 1.293f, 2.804f, 2.209f, 4.37f, 2.411f)
      curveToRelative(3.884f, 0.5f, 7.55f, -2.047f, 8.871f, -6.125f)
      curveToRelative(0.605f, -1.867f, 0.923f, -3.857f, 0.923f, -5.884f)
      verticalLineToRelative(-0.003f)
      curveTo(40.824f, 41.052f, 41.512f, 41.234f, 42.661f, 39.933f)
      close()
    }
    path(fill = SolidColor(Color(0xFFBD6300))) {
      moveTo(32f, 15.024f)
      curveToRelative(-7.941f, 0.022f, -14.252f, 3.061f, -14.933f, 11.351f)
      curveToRelative(-0.048f, 0.586f, -0.121f, 2.684f, 0.047f, 3.724f)
      curveTo(18.48f, 27.646f, 20.993f, 27f, 24f, 27f)
      curveToRelative(2.032f, 0f, 3.887f, 0.758f, 5.298f, 2.006f)
      curveToRelative(1.556f, 1.377f, 3.848f, 1.377f, 5.404f, 0f)
      curveTo(36.113f, 27.758f, 37.968f, 27f, 40f, 27f)
      curveToRelative(3.01f, 0f, 5.518f, 0.699f, 6.884f, 3.156f)
      curveToRelative(0.005f, -0.002f, -0.005f, -0.025f, 0f, -0.027f)
      curveTo(48.098f, 18.995f, 40.86f, 15f, 32f, 15.024f)
      close()
    }
    path(fill = SolidColor(Color(0xFFBD6300))) {
      moveTo(24.5f, 32.5f)
      moveToRelative(-2.5f, 0f)
      arcToRelative(2.5f, 2.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 5f, 0f)
      arcToRelative(2.5f, 2.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, -5f, 0f)
    }
    path(fill = SolidColor(Color(0xFFBD6300))) {
      moveTo(39.5f, 32.5f)
      moveToRelative(-2.5f, 0f)
      arcToRelative(2.5f, 2.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 5f, 0f)
      arcToRelative(2.5f, 2.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, -5f, 0f)
    }
    path(
      fill = SolidColor(Color.White),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(31.961f, 15.024f)
      curveToRelative(-7.731f, 0.027f, -13.9f, 2.917f, -14.851f, 10.717f)
      curveToRelative(0.486f, 0.152f, 0.984f, 0.235f, 1.479f, 0.235f)
      curveToRelative(1.731f, 0f, 3.414f, -0.899f, 4.339f, -2.508f)
      curveToRelative(1.638f, -2.847f, 5.826f, -3.458f, 9.051f, -3.469f)
      curveToRelative(2.551f, -0.009f, 4.625f, -1.933f, 4.923f, -4.402f)
      curveTo(35.275f, 15.203f, 33.592f, 15.019f, 31.961f, 15.024f)
      close()
    }
    path(fill = SolidColor(Color.White)) {
      moveTo(23.26f, 22.461f)
      curveToRelative(-0.482f, 0f, -0.955f, -0.231f, -1.245f, -0.661f)
      curveToRelative(-0.463f, -0.687f, -0.283f, -1.619f, 0.404f, -2.082f)
      curveToRelative(1.55f, -1.046f, 4.25f, -2.355f, 8.316f, -2.654f)
      curveToRelative(0.831f, -0.058f, 1.545f, 0.562f, 1.606f, 1.386f)
      curveToRelative(0.061f, 0.826f, -0.56f, 1.546f, -1.386f, 1.606f)
      curveToRelative(-3.407f, 0.25f, -5.61f, 1.307f, -6.857f, 2.148f)
      curveTo(23.84f, 22.378f, 23.548f, 22.461f, 23.26f, 22.461f)
      close()
    }
    path(fill = SolidColor(Color(0xFFBD6300))) {
      moveTo(33.369f, 47.379f)
      verticalLineToRelative(-0.25f)
      curveToRelative(0.168f, -0.07f, 0.333f, -0.15f, 0.494f, -0.247f)
      lineToRelative(2.567f, -1.54f)
      curveTo(36.78f, 45.132f, 37f, 44.704f, 37f, 44.236f)
      lineToRelative(0f, 0f)
      curveTo(37f, 43.553f, 36.539f, 43f, 35.97f, 43f)
      horizontalLineToRelative(-7.94f)
      curveTo(27.461f, 43f, 27f, 43.553f, 27f, 44.236f)
      lineToRelative(0f, 0f)
      curveToRelative(0f, 0.468f, 0.22f, 0.896f, 0.569f, 1.105f)
      lineToRelative(2.567f, 1.54f)
      curveToRelative(0.076f, 0.046f, 0.155f, 0.08f, 0.233f, 0.12f)
      verticalLineToRelative(0.377f)
      lineTo(25.948f, 51.8f)
      curveToRelative(0.659f, 0.796f, 1.412f, 1.487f, 2.22f, 2.022f)
      lineToRelative(3.701f, -3.701f)
      lineToRelative(3.868f, 3.868f)
      curveToRelative(0.863f, -0.487f, 1.646f, -1.14f, 2.314f, -1.928f)
      lineTo(33.369f, 47.379f)
      close()
    }
  }.build()
}

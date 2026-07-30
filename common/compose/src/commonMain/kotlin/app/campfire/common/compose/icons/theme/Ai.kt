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

val CampfireIcons.Theme.Ai: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "Ai",
    defaultWidth = 64.dp,
    defaultHeight = 64.dp,
    viewportWidth = 64f,
    viewportHeight = 64f,
  ).apply {
    path(fill = SolidColor(Color(0xFFFD3C4F))) {
      moveTo(14.485f, 21.141f)
      lineToRelative(-5.922f, 2.192f)
      curveToRelative(-1.548f, 0.573f, -2.549f, 2.01f, -2.549f, 3.661f)
      curveToRelative(0f, 1.651f, 1.001f, 3.088f, 2.549f, 3.661f)
      lineToRelative(2.739f, 1.013f)
      curveToRelative(0.676f, -4.283f, 2.468f, -8.198f, 5.064f, -11.443f)
      curveTo(15.771f, 20.581f, 15.147f, 20.896f, 14.485f, 21.141f)
      close()
    }
    path(fill = SolidColor(Color(0xFFFD3C4F))) {
      moveTo(30.668f, 8.549f)
      curveTo(30.095f, 7f, 28.658f, 6f, 27.007f, 6f)
      reflectiveCurveToRelative(-3.088f, 1f, -3.661f, 2.549f)
      lineToRelative(-2.191f, 5.922f)
      curveToRelative(-0.246f, 0.664f, -0.562f, 1.29f, -0.92f, 1.887f)
      curveToRelative(3.248f, -2.595f, 7.165f, -4.386f, 11.45f, -5.059f)
      lineTo(30.668f, 8.549f)
      close()
    }
    path(fill = SolidColor(Color(0xFFE57FCF))) {
      moveTo(33.151f, 15.145f)
      curveToRelative(-0.1f, -0.224f, -0.206f, -0.443f, -0.292f, -0.674f)
      lineToRelative(-1.174f, -3.172f)
      curveToRelative(-4.285f, 0.673f, -8.202f, 2.463f, -11.45f, 5.059f)
      curveToRelative(-0.953f, 1.589f, -2.279f, 2.914f, -3.869f, 3.867f)
      curveToRelative(-2.596f, 3.246f, -4.388f, 7.16f, -5.064f, 11.443f)
      lineToRelative(3.183f, 1.178f)
      curveToRelative(0.227f, 0.084f, 0.443f, 0.189f, 0.663f, 0.287f)
      curveTo(16.238f, 23.713f, 23.73f, 16.226f, 33.151f, 15.145f)
      close()
    }
    path(fill = SolidColor(Color(0xFFA389E0))) {
      moveTo(35.5f, 19f)
      curveToRelative(0.171f, 0f, 0.337f, 0.02f, 0.508f, 0.026f)
      curveToRelative(-1.212f, -1.064f, -2.188f, -2.381f, -2.856f, -3.88f)
      curveToRelative(-9.422f, 1.081f, -16.913f, 8.568f, -18.003f, 17.987f)
      curveToRelative(1.496f, 0.665f, 2.812f, 1.636f, 3.876f, 2.844f)
      curveTo(19.019f, 35.816f, 19f, 35.661f, 19f, 35.5f)
      curveTo(19f, 26.387f, 26.387f, 19f, 35.5f, 19f)
      close()
    }
    path(fill = SolidColor(Color(0xFF68E5FD))) {
      moveTo(45.451f, 23.332f)
      lineToRelative(-5.922f, -2.192f)
      curveToRelative(-1.313f, -0.486f, -2.495f, -1.214f, -3.522f, -2.115f)
      curveTo(35.837f, 19.02f, 35.671f, 19f, 35.5f, 19f)
      curveTo(26.387f, 19f, 19f, 26.387f, 19f, 35.5f)
      curveToRelative(0f, 0.161f, 0.019f, 0.316f, 0.024f, 0.476f)
      curveToRelative(0.908f, 1.03f, 1.642f, 2.219f, 2.131f, 3.539f)
      lineToRelative(2.191f, 5.922f)
      curveToRelative(0.573f, 1.548f, 2.01f, 2.549f, 3.661f, 2.549f)
      reflectiveCurveToRelative(3.088f, -1.001f, 3.661f, -2.549f)
      lineToRelative(2.192f, -5.922f)
      curveToRelative(1.145f, -3.094f, 3.576f, -5.525f, 6.67f, -6.67f)
      lineToRelative(5.922f, -2.191f)
      curveTo(47f, 30.081f, 48f, 28.644f, 48f, 26.993f)
      curveTo(48f, 25.342f, 47f, 23.905f, 45.451f, 23.332f)
      close()
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(43.716f, 25.964f)
      lineToRelative(-5.922f, 2.191f)
      curveToRelative(-4.464f, 1.652f, -7.972f, 5.16f, -9.624f, 9.624f)
      lineToRelative(-2.192f, 5.922f)
      curveToRelative(-0.538f, 1.453f, -0.337f, 2.986f, 0.382f, 4.23f)
      curveToRelative(0.211f, 0.034f, 0.426f, 0.055f, 0.646f, 0.055f)
      curveToRelative(1.651f, 0f, 3.088f, -1.001f, 3.661f, -2.549f)
      lineToRelative(2.192f, -5.922f)
      curveToRelative(1.145f, -3.094f, 3.576f, -5.525f, 6.67f, -6.67f)
      lineToRelative(5.922f, -2.191f)
      curveTo(47f, 30.081f, 48f, 28.644f, 48f, 26.993f)
      curveToRelative(0f, -0.22f, -0.021f, -0.435f, -0.055f, -0.646f)
      curveTo(46.701f, 25.628f, 45.17f, 25.428f, 43.716f, 25.964f)
      close()
    }
    path(
      fill = SolidColor(Color.White),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(25.844f, 16.206f)
      lineToRelative(2.191f, -5.922f)
      curveToRelative(0.537f, -1.453f, 0.337f, -2.985f, -0.382f, -4.229f)
      curveTo(27.443f, 6.021f, 27.227f, 6f, 27.007f, 6f)
      curveToRelative(-1.651f, 0f, -3.088f, 1f, -3.661f, 2.549f)
      lineToRelative(-2.191f, 5.922f)
      curveToRelative(-1.145f, 3.094f, -3.576f, 5.525f, -6.67f, 6.67f)
      lineToRelative(-5.922f, 2.192f)
      curveToRelative(-1.548f, 0.573f, -2.549f, 2.01f, -2.549f, 3.661f)
      curveToRelative(0f, 0.224f, 0.022f, 0.444f, 0.058f, 0.659f)
      curveToRelative(0.753f, 0.434f, 1.607f, 0.682f, 2.492f, 0.682f)
      curveToRelative(0.577f, 0f, 1.163f, -0.101f, 1.735f, -0.312f)
      lineToRelative(5.921f, -2.191f)
      curveTo(20.684f, 24.178f, 24.192f, 20.67f, 25.844f, 16.206f)
      close()
    }
    path(
      stroke = SolidColor(Color.White),
      strokeLineWidth = 3f,
      strokeLineCap = StrokeCap.Round,
    ) {
      moveTo(18.513f, 23.616f)
      curveToRelative(2.724f, -1.655f, 4.876f, -4.131f, 6.133f, -7.102f)
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
    path(fill = SolidColor(Color(0xFFFD3C4F))) {
      moveTo(56.26f, 44.528f)
      lineTo(53.699f, 43.6f)
      curveToRelative(-0.033f, -0.012f, -0.061f, -0.033f, -0.094f, -0.046f)
      curveToRelative(-1.515f, 4.214f, -4.857f, 7.551f, -9.073f, 9.059f)
      curveToRelative(0.014f, 0.036f, 0.038f, 0.067f, 0.051f, 0.104f)
      lineToRelative(0.927f, 2.561f)
      curveToRelative(0.385f, 1.063f, 1.364f, 1.75f, 2.494f, 1.75f)
      horizontalLineToRelative(0.001f)
      curveToRelative(1.13f, 0f, 2.11f, -0.686f, 2.495f, -1.75f)
      lineToRelative(0.928f, -2.562f)
      curveToRelative(0.383f, -1.059f, 1.211f, -1.887f, 2.271f, -2.271f)
      lineToRelative(2.56f, -0.927f)
      curveToRelative(1.063f, -0.385f, 1.751f, -1.365f, 1.751f, -2.496f)
      curveTo(58.011f, 45.892f, 57.323f, 44.913f, 56.26f, 44.528f)
      close()
    }
    path(fill = SolidColor(Color(0xFFE57FCF))) {
      moveTo(53.605f, 43.554f)
      curveToRelative(-1.012f, -0.398f, -1.805f, -1.198f, -2.176f, -2.224f)
      lineToRelative(-0.928f, -2.563f)
      curveToRelative(-0.004f, -0.01f, -0.009f, -0.018f, -0.013f, -0.028f)
      curveToRelative(-0.127f, 5.901f, -4.892f, 10.652f, -10.799f, 10.752f)
      curveToRelative(0.021f, 0.008f, 0.04f, 0.02f, 0.062f, 0.028f)
      lineToRelative(2.56f, 0.927f)
      curveToRelative(1.023f, 0.371f, 1.821f, 1.161f, 2.221f, 2.167f)
      curveTo(48.748f, 51.105f, 52.09f, 47.767f, 53.605f, 43.554f)
      close()
    }
    path(fill = SolidColor(Color(0xFFA389E0))) {
      moveTo(48.005f, 37.018f)
      lineTo(48.005f, 37.018f)
      curveToRelative(-1.131f, 0f, -2.11f, 0.687f, -2.495f, 1.75f)
      lineToRelative(-0.927f, 2.561f)
      curveToRelative(-0.384f, 1.06f, -1.212f, 1.888f, -2.272f, 2.272f)
      lineToRelative(-2.56f, 0.927f)
      curveTo(38.687f, 44.913f, 38f, 45.892f, 38f, 47.023f)
      curveToRelative(0f, 1.108f, 0.662f, 2.067f, 1.689f, 2.468f)
      curveToRelative(5.907f, -0.1f, 10.672f, -4.851f, 10.799f, -10.752f)
      curveTo(50.095f, 37.693f, 49.126f, 37.018f, 48.005f, 37.018f)
      close()
    }
  }.build()
}

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

val CampfireIcons.Theme.Campfire: ImageVector by lazy(LazyThreadSafetyMode.PUBLICATION) {
  ImageVector.Builder(
    name = "Theme.Campfire",
    defaultWidth = 64.dp,
    defaultHeight = 64.dp,
    viewportWidth = 64f,
    viewportHeight = 64f,
  ).apply {
    path(fill = SolidColor(Color(0xFFBD6300))) {
      moveTo(46.841f, 57.003f)
      curveToRelative(1.815f, 0.067f, 3.505f, -1.117f, 4.003f, -2.946f)
      curveToRelative(0.581f, -2.131f, -0.676f, -4.331f, -2.807f, -4.912f)
      lineToRelative(-29.721f, -8.103f)
      curveToRelative(-2.124f, -0.581f, -4.331f, 0.675f, -4.912f, 2.807f)
      curveToRelative(-0.581f, 2.131f, 0.676f, 4.331f, 2.807f, 4.912f)
      lineToRelative(29.721f, 8.103f)
      curveTo(46.235f, 56.946f, 46.539f, 56.992f, 46.841f, 57.003f)
      close()
    }
    path(fill = SolidColor(Color(0xFFFFA500))) {
      moveTo(47f, 53f)
      moveToRelative(-4f, 0f)
      arcToRelative(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 8f, 0f)
      arcToRelative(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, -8f, 0f)
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(15f, 61f)
      arcToRelative(17f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, 34f, 0f)
      arcToRelative(17f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, -34f, 0f)
      close()
    }
    path(fill = SolidColor(Color(0xFFDA7200))) {
      moveTo(17.104f, 57.003f)
      curveToRelative(-1.815f, 0.067f, -3.505f, -1.117f, -4.003f, -2.946f)
      curveToRelative(-0.581f, -2.131f, 0.676f, -4.331f, 2.807f, -4.912f)
      lineToRelative(29.721f, -8.103f)
      curveToRelative(2.124f, -0.581f, 4.331f, 0.675f, 4.912f, 2.807f)
      curveToRelative(0.581f, 2.131f, -0.676f, 4.331f, -2.807f, 4.912f)
      lineToRelative(-29.721f, 8.103f)
      curveTo(17.71f, 56.946f, 17.406f, 56.992f, 17.104f, 57.003f)
      close()
    }
    path(fill = SolidColor(Color(0xFFFFA500))) {
      moveTo(16.949f, 53f)
      moveToRelative(-4f, 0f)
      arcToRelative(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 8f, 0f)
      arcToRelative(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, -8f, 0f)
    }
    path(fill = SolidColor(Color(0xFFFD3C4F))) {
      moveTo(42.85f, 16.773f)
      curveToRelative(-0.66f, 1.306f, -1.402f, 2.456f, -2.076f, 3.386f)
      curveToRelative(-0.46f, 0.634f, -1.467f, 0.236f, -1.364f, -0.54f)
      curveToRelative(0.449f, -3.378f, 0.16f, -8.975f, -4.992f, -13.813f)
      curveToRelative(-1.209f, -1.135f, -3.103f, -1.034f, -4.281f, 0.133f)
      curveToRelative(-6.872f, 6.8f, -17.059f, 12.366f, -16.871f, 23.117f)
      curveToRelative(0.162f, 9.226f, 7.389f, 17.062f, 16.581f, 17.871f)
      curveToRelative(10.817f, 0.953f, 19.891f, -7.546f, 19.891f, -18.164f)
      curveToRelative(0f, -3.923f, -1.847f, -8.226f, -4.623f, -12.15f)
      curveTo(44.542f, 15.804f, 43.296f, 15.889f, 42.85f, 16.773f)
      close()
    }
    path(
      stroke = SolidColor(Color.White),
      strokeLineWidth = 3f,
      strokeLineCap = StrokeCap.Round,
      strokeLineJoin = StrokeJoin.Round,
    ) {
      moveTo(19.634f, 21.58f)
      curveToRelative(1.063f, -1.549f, 2.424f, -3.006f, 3.962f, -4.456f)
    }
    path(fill = SolidColor(Color(0xFFFFCE29))) {
      moveTo(40f, 39f)
      curveToRelative(0f, 4.971f, -3.582f, 8f, -8f, 8f)
      reflectiveCurveToRelative(-8f, -3.029f, -8f, -8f)
      curveToRelative(0f, -4.149f, 3.825f, -8.776f, 6.185f, -11.236f)
      curveToRelative(1.002f, -1.044f, 2.627f, -1.044f, 3.629f, 0f)
      curveTo(36.175f, 30.224f, 40f, 34.851f, 40f, 39f)
      close()
    }
    path(
      fill = SolidColor(Color.White),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(28.416f, 14.201f)
      curveToRelative(1.754f, -1.502f, 3.567f, -3.055f, 5.237f, -4.707f)
      curveToRelative(0.85f, -0.841f, 1.312f, -1.914f, 1.431f, -3.017f)
      curveToRelative(-0.216f, -0.224f, -0.431f, -0.449f, -0.667f, -0.671f)
      curveToRelative(-1.209f, -1.135f, -3.103f, -1.034f, -4.281f, 0.133f)
      curveToRelative(-6.872f, 6.8f, -17.059f, 12.366f, -16.871f, 23.117f)
      curveToRelative(0.03f, 1.698f, 0.303f, 3.348f, 0.779f, 4.917f)
      curveToRelative(2.424f, -0.382f, 4.264f, -2.481f, 4.22f, -5.005f)
      curveTo(18.167f, 23.388f, 22.034f, 19.666f, 28.416f, 14.201f)
      close()
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(48.873f, 23.85f)
      curveToRelative(-2.348f, 0.411f, -4.136f, 2.447f, -4.136f, 4.913f)
      curveToRelative(0f, 3.703f, -1.567f, 7.263f, -4.301f, 9.766f)
      curveToRelative(-2.77f, 2.536f, -6.364f, 3.751f, -10.151f, 3.418f)
      curveToRelative(-2.503f, -0.222f, -4.73f, 1.462f, -5.287f, 3.849f)
      curveToRelative(1.529f, 0.591f, 3.154f, 0.982f, 4.848f, 1.132f)
      curveToRelative(10.817f, 0.953f, 19.891f, -7.546f, 19.891f, -18.164f)
      curveTo(49.737f, 27.171f, 49.42f, 25.516f, 48.873f, 23.85f)
      close()
    }
  }.build()
}

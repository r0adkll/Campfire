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

val CampfireIcons.Theme.Snowflake: ImageVector by lazy(LazyThreadSafetyMode.PUBLICATION) {
  ImageVector.Builder(
    name = "Theme.Snowflake",
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
      moveTo(10.5f, 61f)
      arcToRelative(21.5f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, 43f, 0f)
      arcToRelative(21.5f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, -43f, 0f)
      close()
    }
    path(fill = SolidColor(Color(0xFF37D0EE))) {
      moveTo(32f, 55f)
      curveToRelative(-1.657f, 0f, -3f, -1.343f, -3f, -3f)
      verticalLineTo(9f)
      curveToRelative(0f, -1.657f, 1.343f, -3f, 3f, -3f)
      reflectiveCurveToRelative(3f, 1.343f, 3f, 3f)
      verticalLineToRelative(43f)
      curveTo(35f, 53.657f, 33.657f, 55f, 32f, 55f)
      close()
    }
    path(
      stroke = SolidColor(Color(0xFF37D0EE)),
      strokeLineWidth = 5f,
      strokeLineCap = StrokeCap.Round,
      strokeLineJoin = StrokeJoin.Round,
    ) {
      moveTo(26.5f, 48.52f)
      lineToRelative(5.499f, -5f)
      lineToRelative(5.507f, 5f)
    }
    path(
      stroke = SolidColor(Color(0xFF37D0EE)),
      strokeLineWidth = 5f,
      strokeLineCap = StrokeCap.Round,
      strokeLineJoin = StrokeJoin.Round,
    ) {
      moveTo(26.5f, 12.52f)
      lineToRelative(5.499f, 5f)
      lineToRelative(5.507f, -5f)
    }
    path(fill = SolidColor(Color(0xFF37D0EE))) {
      moveTo(13.374f, 44.23f)
      curveToRelative(-1.038f, 0f, -2.047f, -0.539f, -2.603f, -1.503f)
      curveToRelative(-0.827f, -1.436f, -0.334f, -3.27f, 1.103f, -4.097f)
      lineToRelative(37.26f, -21.463f)
      curveToRelative(1.435f, -0.828f, 3.269f, -0.333f, 4.097f, 1.102f)
      curveToRelative(0.827f, 1.436f, 0.334f, 3.27f, -1.103f, 4.097f)
      lineToRelative(-37.26f, 21.463f)
      curveTo(14.396f, 44.101f, 13.882f, 44.23f, 13.374f, 44.23f)
      close()
    }
    path(
      stroke = SolidColor(Color(0xFF37D0EE)),
      strokeLineWidth = 5f,
      strokeLineCap = StrokeCap.Round,
      strokeLineJoin = StrokeJoin.Round,
    ) {
      moveTo(13.641f, 34.727f)
      lineToRelative(7.078f, 2.269f)
      lineToRelative(-1.584f, 7.267f)
    }
    path(
      stroke = SolidColor(Color(0xFF37D0EE)),
      strokeLineWidth = 5f,
      strokeLineCap = StrokeCap.Round,
      strokeLineJoin = StrokeJoin.Round,
    ) {
      moveTo(44.836f, 16.758f)
      lineToRelative(-1.588f, 7.261f)
      lineToRelative(7.081f, 2.275f)
    }
    path(fill = SolidColor(Color(0xFF37D0EE))) {
      moveTo(50.628f, 44.255f)
      curveToRelative(-0.508f, 0f, -1.022f, -0.129f, -1.494f, -0.401f)
      lineToRelative(-37.26f, -21.463f)
      curveToRelative(-1.437f, -0.827f, -1.93f, -2.661f, -1.103f, -4.097f)
      curveToRelative(0.828f, -1.436f, 2.66f, -1.929f, 4.097f, -1.102f)
      lineToRelative(37.26f, 21.463f)
      curveToRelative(1.437f, 0.827f, 1.93f, 2.662f, 1.103f, 4.097f)
      curveTo(52.675f, 43.716f, 51.666f, 44.255f, 50.628f, 44.255f)
      close()
    }
    path(
      stroke = SolidColor(Color(0xFF37D0EE)),
      strokeLineWidth = 5f,
      strokeLineCap = StrokeCap.Round,
      strokeLineJoin = StrokeJoin.Round,
    ) {
      moveTo(13.641f, 26.294f)
      lineToRelative(7.078f, -2.269f)
      lineToRelative(-1.584f, -7.267f)
    }
    path(
      stroke = SolidColor(Color(0xFF37D0EE)),
      strokeLineWidth = 5f,
      strokeLineCap = StrokeCap.Round,
      strokeLineJoin = StrokeJoin.Round,
    ) {
      moveTo(44.836f, 44.263f)
      lineToRelative(-1.588f, -7.261f)
      lineToRelative(7.081f, -2.275f)
    }
    path(fill = SolidColor(Color(0xFF37D0EE))) {
      moveTo(52.128f, 38.655f)
      lineTo(37.99f, 30.51f)
      lineToRelative(14.138f, -8.144f)
      curveToRelative(1.437f, -0.827f, 1.93f, -2.661f, 1.103f, -4.097f)
      curveToRelative(-0.828f, -1.436f, -2.662f, -1.93f, -4.097f, -1.102f)
      lineTo(35f, 25.309f)
      verticalLineTo(9f)
      curveToRelative(0f, -1.657f, -1.343f, -3f, -3f, -3f)
      reflectiveCurveToRelative(-3f, 1.343f, -3f, 3f)
      verticalLineToRelative(16.332f)
      lineToRelative(-14.132f, -8.141f)
      curveToRelative(-1.437f, -0.827f, -3.269f, -0.333f, -4.097f, 1.102f)
      curveToRelative(-0.827f, 1.436f, -0.334f, 3.27f, 1.103f, 4.097f)
      lineToRelative(14.096f, 8.12f)
      lineToRelative(-14.096f, 8.12f)
      curveToRelative(-1.437f, 0.827f, -1.93f, 2.661f, -1.103f, 4.097f)
      curveToRelative(0.556f, 0.964f, 1.564f, 1.503f, 2.603f, 1.503f)
      curveToRelative(0.508f, 0f, 1.022f, -0.129f, 1.494f, -0.401f)
      lineTo(29f, 35.689f)
      verticalLineTo(52f)
      curveToRelative(0f, 1.657f, 1.343f, 3f, 3f, 3f)
      reflectiveCurveToRelative(3f, -1.343f, 3f, -3f)
      verticalLineTo(35.712f)
      lineToRelative(14.134f, 8.142f)
      curveToRelative(0.472f, 0.272f, 0.986f, 0.401f, 1.494f, 0.401f)
      curveToRelative(1.038f, 0f, 2.047f, -0.539f, 2.603f, -1.503f)
      curveTo(54.058f, 41.316f, 53.564f, 39.482f, 52.128f, 38.655f)
      close()
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(53.608f, 41.628f)
      curveToRelative(-0.423f, -0.853f, -1.091f, -1.595f, -1.978f, -2.106f)
      lineTo(37.496f, 31.38f)
      curveToRelative(-1.548f, -0.891f, -3.454f, -0.89f, -4.998f, 0.003f)
      curveTo(30.952f, 32.277f, 30f, 33.927f, 30f, 35.712f)
      verticalLineTo(52f)
      curveToRelative(0f, 1.023f, 0.31f, 1.974f, 0.838f, 2.766f)
      curveTo(31.195f, 54.916f, 31.588f, 55f, 32f, 55f)
      curveToRelative(1.657f, 0f, 3f, -1.343f, 3f, -3f)
      verticalLineTo(35.712f)
      lineToRelative(14.134f, 8.142f)
      curveToRelative(0.472f, 0.272f, 0.986f, 0.401f, 1.494f, 0.401f)
      curveToRelative(1.038f, 0f, 2.047f, -0.539f, 2.603f, -1.503f)
      curveTo(53.436f, 42.395f, 53.56f, 42.013f, 53.608f, 41.628f)
      close()
    }
    path(
      fill = SolidColor(Color.White),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(34f, 25.332f)
      verticalLineTo(9f)
      curveToRelative(0f, -1.023f, -0.31f, -1.974f, -0.838f, -2.766f)
      curveTo(32.805f, 6.084f, 32.412f, 6f, 32f, 6f)
      curveToRelative(-1.657f, 0f, -3f, 1.343f, -3f, 3f)
      verticalLineToRelative(16.332f)
      lineToRelative(-14.132f, -8.141f)
      curveToRelative(-1.437f, -0.827f, -3.269f, -0.333f, -4.097f, 1.102f)
      curveToRelative(-0.206f, 0.357f, -0.329f, 0.739f, -0.377f, 1.124f)
      curveToRelative(0.423f, 0.853f, 1.091f, 1.595f, 1.978f, 2.106f)
      lineToRelative(14.132f, 8.141f)
      curveToRelative(0.772f, 0.445f, 1.635f, 0.667f, 2.496f, 0.667f)
      curveToRelative(0.864f, 0f, 1.729f, -0.224f, 2.502f, -0.671f)
      curveTo(33.048f, 28.768f, 34f, 27.118f, 34f, 25.332f)
      close()
    }
    path(
      stroke = SolidColor(Color.White),
      strokeLineWidth = 3f,
      strokeLineCap = StrokeCap.Round,
      strokeLineJoin = StrokeJoin.Round,
    ) {
      moveTo(31.5f, 14.5f)
      lineTo(31.5f, 19.5f)
    }
  }.build()
}

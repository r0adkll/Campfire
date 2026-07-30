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

val CampfireIcons.Theme.Trekking: ImageVector by lazy(LazyThreadSafetyMode.PUBLICATION) {
  ImageVector.Builder(
    name = "Theme.Trekking",
    defaultWidth = 64.dp,
    defaultHeight = 64.dp,
    viewportWidth = 64f,
    viewportHeight = 64f,
  ).apply {
    path(
      stroke = SolidColor(Color(0xFFFFCE29)),
      strokeLineWidth = 3f,
      strokeLineCap = StrokeCap.Round,
      strokeLineJoin = StrokeJoin.Round,
    ) {
      moveTo(49.5f, 15.5f)
      lineTo(49.5f, 55.5f)
    }
    path(fill = SolidColor(Color(0xFF9C34C2))) {
      moveTo(43.502f, 29f)
      curveToRelative(-0.754f, 0f, -1.493f, -0.34f, -1.981f, -0.973f)
      lineToRelative(-8.314f, -8.183f)
      curveToRelative(-0.843f, -1.093f, -0.641f, -2.663f, 0.452f, -3.507f)
      curveToRelative(1.094f, -0.841f, 2.797f, -0.504f, 3.64f, 0.59f)
      lineToRelative(6.85f, 6.323f)
      lineToRelative(5.11f, -2.92f)
      curveToRelative(1.198f, -0.686f, 2.725f, -0.27f, 3.411f, 0.93f)
      curveToRelative(0.685f, 1.199f, 0.269f, 2.726f, -0.931f, 3.411f)
      lineToRelative(-7f, 4f)
      curveTo(44.351f, 28.893f, 43.924f, 29f, 43.502f, 29f)
      close()
    }
    path(fill = SolidColor(Color(0xFF9C34C2))) {
      moveTo(41.001f, 57f)
      curveToRelative(-1.381f, 0f, -2.623f, -0.959f, -2.929f, -2.363f)
      lineToRelative(-1.785f, -10.555f)
      lineToRelative(-8.971f, -7.352f)
      curveToRelative(-1.231f, -1.108f, -1.642f, -4.006f, -0.533f, -5.237f)
      reflectiveCurveToRelative(5.499f, -2.602f, 6.73f, -1.493f)
      lineToRelative(7.507f, 10.27f)
      curveToRelative(0.468f, 0.421f, 0.791f, 0.978f, 0.925f, 1.593f)
      lineToRelative(1.99f, 11.5f)
      curveToRelative(0.352f, 1.619f, -0.675f, 3.217f, -2.294f, 3.569f)
      curveTo(41.427f, 56.978f, 41.212f, 57f, 41.001f, 57f)
      close()
    }
    path(fill = SolidColor(Color(0xFF9C34C2))) {
      moveTo(23.004f, 56f)
      curveToRelative(-0.451f, 0f, -0.909f, -0.102f, -1.339f, -0.317f)
      curveToRelative(-1.482f, -0.741f, -2.083f, -2.543f, -1.341f, -4.025f)
      lineTo(25.514f, 42f)
      verticalLineToRelative(-9f)
      curveToRelative(0.297f, -1.63f, 3.908f, -4.243f, 5.537f, -3.952f)
      curveToRelative(1.63f, 0.296f, 2.711f, 1.858f, 2.415f, 3.488f)
      lineToRelative(-2f, 11f)
      curveToRelative(-0.051f, 0.28f, -0.141f, 0.551f, -0.269f, 0.805f)
      lineToRelative(-5.508f, 10f)
      curveTo(25.164f, 55.393f, 24.104f, 56f, 23.004f, 56f)
      close()
    }
    path(fill = SolidColor(Color(0xFF9C34C2))) {
      moveTo(27.302f, 18.621f)
      curveToRelative(0.268f, -2.089f, 2.063f, -3.645f, 4.168f, -3.611f)
      curveToRelative(1.301f, 0.021f, 2.566f, 0.079f, 3.08f, 0.215f)
      lineToRelative(0f, 0f)
      curveToRelative(2.513f, 0.666f, 4.021f, 2.787f, 3.958f, 5.384f)
      lineToRelative(-1.899f, 14.933f)
      lineToRelative(-4.181f, 2.287f)
      curveToRelative(-3.618f, 0.926f, -7.153f, -1.665f, -6.903f, -5.392f)
      lineTo(27.302f, 18.621f)
      close()
    }
    path(
      fill = SolidColor(Color.White),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(30.345f, 15.164f)
      curveToRelative(-1.587f, 0.423f, -2.824f, 1.755f, -3.043f, 3.456f)
      lineToRelative(-1.776f, 13.816f)
      curveToRelative(-0.038f, 0.914f, -0.019f, 1.751f, 0f, 2.579f)
      curveToRelative(2.417f, -0.07f, 4.955f, -1.914f, 5.273f, -4.387f)
      lineToRelative(1.404f, -10.927f)
      curveTo(32.435f, 17.903f, 31.667f, 16.218f, 30.345f, 15.164f)
      close()
    }
    path(fill = SolidColor(Color(0xFFFFCE29))) {
      moveTo(34.519f, 9f)
      moveToRelative(-5f, 0f)
      arcToRelative(5f, 5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 10f, 0f)
      arcToRelative(5f, 5f, 0f, isMoreThanHalf = true, isPositiveArc = true, -10f, 0f)
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(12.014f, 61f)
      arcToRelative(19f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, 38f, 0f)
      arcToRelative(19f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, -38f, 0f)
      close()
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(43.218f, 56.016f)
      curveToRelative(0.63f, -0.694f, 0.932f, -1.667f, 0.718f, -2.653f)
      lineToRelative(-1.99f, -11.5f)
      curveToRelative(-0.134f, -0.615f, -0.457f, -1.172f, -0.925f, -1.593f)
      lineToRelative(-1.775f, -2.428f)
      curveToRelative(-1.682f, 1.102f, -2.606f, 3.152f, -2.153f, 5.235f)
      lineToRelative(1.451f, 9.019f)
      curveTo(39.037f, 54.364f, 41.001f, 55.922f, 43.218f, 56.016f)
      close()
    }
    path(fill = SolidColor(Color(0xFF98C900))) {
      moveTo(23.104f, 31f)
      horizontalLineToRelative(-4.101f)
      curveToRelative(-1.8f, 0f, -2.978f, -1.652f, -2.978f, -3.362f)
      curveToRelative(0f, -5.537f, 4.39f, -12.639f, 8.501f, -12.639f)
      curveToRelative(1.822f, 0f, 3.226f, 1.616f, 2.969f, 3.422f)
      lineToRelative(-1.421f, 10f)
      curveTo(25.864f, 29.901f, 24.598f, 31f, 23.104f, 31f)
      close()
    }
    path(
      stroke = SolidColor(Color.White),
      strokeLineWidth = 3f,
      strokeLineCap = StrokeCap.Round,
      strokeLineJoin = StrokeJoin.Round,
    ) {
      moveTo(30.461f, 20.534f)
      lineTo(30.056f, 24.461f)
    }
  }.build()
}

// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Theme.Train: ImageVector by lazy(LazyThreadSafetyMode.PUBLICATION) {
  ImageVector.Builder(
    name = "Theme.Train",
    defaultWidth = 64.dp,
    defaultHeight = 64.dp,
    viewportWidth = 64f,
    viewportHeight = 64f,
  ).apply {
    path(fill = SolidColor(Color(0xFF9C34C2))) {
      moveTo(14.669f, 54.836f)
      lineTo(14.669f, 54.836f)
      curveToRelative(-1.608f, -0.401f, -2.586f, -2.029f, -2.185f, -3.637f)
      lineToRelative(1.936f, -7.762f)
      lineToRelative(5.822f, 1.452f)
      lineToRelative(-1.936f, 7.762f)
      curveTo(17.905f, 54.259f, 16.276f, 55.237f, 14.669f, 54.836f)
      close()
    }
    path(fill = SolidColor(Color(0xFF9C34C2))) {
      moveTo(50.012f, 54.653f)
      lineTo(50.012f, 54.653f)
      curveToRelative(-1.561f, 0.556f, -3.277f, -0.259f, -3.832f, -1.82f)
      lineToRelative(-3.018f, -8.479f)
      lineToRelative(5.653f, -2.012f)
      lineToRelative(3.018f, 8.479f)
      curveTo(52.388f, 52.381f, 51.573f, 54.097f, 50.012f, 54.653f)
      close()
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(13f, 61.009f)
      arcToRelative(19f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, 38f, 0f)
      arcToRelative(19f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, -38f, 0f)
      close()
    }
    path(fill = SolidColor(Color(0xFFFD3C4F))) {
      moveTo(47f, 47f)
      horizontalLineTo(17f)
      curveToRelative(-3.314f, 0f, -6f, -2.686f, -6f, -6f)
      verticalLineTo(23.046f)
      curveToRelative(0f, -3.307f, 0.863f, -6.556f, 2.503f, -9.427f)
      lineToRelative(2.905f, -5.084f)
      curveTo(18.011f, 5.731f, 20.993f, 4f, 24.223f, 4f)
      horizontalLineToRelative(15.554f)
      curveToRelative(3.23f, 0f, 6.212f, 1.731f, 7.814f, 4.535f)
      lineToRelative(2.905f, 5.084f)
      curveTo(52.137f, 16.49f, 53f, 19.739f, 53f, 23.046f)
      verticalLineTo(41f)
      curveTo(53f, 44.314f, 50.314f, 47f, 47f, 47f)
      close()
    }
    path(
      fill = SolidColor(Color.White),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(16.409f, 8.535f)
      lineToRelative(-2.905f, 5.084f)
      curveTo(11.863f, 16.49f, 11f, 19.739f, 11f, 23.046f)
      verticalLineToRelative(5f)
      curveToRelative(2.761f, 0f, 5f, -2.238f, 5f, -5f)
      curveToRelative(0f, -2.433f, 0.638f, -4.834f, 1.845f, -6.946f)
      lineToRelative(2.905f, -5.084f)
      curveTo(21.46f, 9.772f, 22.791f, 9f, 24.223f, 9f)
      horizontalLineToRelative(3.554f)
      curveToRelative(2.761f, 0f, 5f, -2.238f, 5f, -5f)
      horizontalLineToRelative(-8.554f)
      curveTo(20.993f, 4f, 18.011f, 5.73f, 16.409f, 8.535f)
      close()
    }
    path(fill = SolidColor(Color(0xFFCD2E42))) {
      moveTo(47f, 47f)
      horizontalLineTo(17f)
      curveToRelative(-3.314f, 0f, -6f, -2.686f, -6f, -6f)
      verticalLineToRelative(-6f)
      horizontalLineToRelative(42f)
      verticalLineToRelative(6f)
      curveTo(53f, 44.314f, 50.314f, 47f, 47f, 47f)
      close()
    }
    path(fill = SolidColor(Color(0xFFCD2E42))) {
      moveTo(21f, 35f)
      moveToRelative(-6f, 0f)
      arcToRelative(6f, 6f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 0f)
      arcToRelative(6f, 6f, 0f, isMoreThanHalf = true, isPositiveArc = true, -12f, 0f)
    }
    path(fill = SolidColor(Color(0xFFCD2E42))) {
      moveTo(42f, 35f)
      moveToRelative(-6f, 0f)
      arcToRelative(6f, 6f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 0f)
      arcToRelative(6f, 6f, 0f, isMoreThanHalf = true, isPositiveArc = true, -12f, 0f)
    }
    path(fill = SolidColor(Color(0xFFFFCE29))) {
      moveTo(42f, 35f)
      moveToRelative(-3f, 0f)
      arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, 6f, 0f)
      arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, -6f, 0f)
    }
    path(fill = SolidColor(Color(0xFFFFCE29))) {
      moveTo(21f, 35f)
      moveToRelative(-3f, 0f)
      arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, 6f, 0f)
      arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, -6f, 0f)
    }
    path(fill = SolidColor(Color(0xFF9C34C2))) {
      moveTo(45f, 25f)
      horizontalLineTo(19f)
      curveToRelative(-1.657f, 0f, -3f, -1.343f, -3f, -3f)
      verticalLineToRelative(-1.407f)
      curveToRelative(0f, -1.044f, 0.272f, -2.07f, 0.791f, -2.977f)
      lineToRelative(2.058f, -3.601f)
      curveTo(19.56f, 12.769f, 20.886f, 12f, 22.321f, 12f)
      horizontalLineToRelative(19.357f)
      curveToRelative(1.435f, 0f, 2.761f, 0.769f, 3.473f, 2.015f)
      lineToRelative(2.058f, 3.601f)
      curveTo(47.728f, 18.523f, 48f, 19.549f, 48f, 20.593f)
      verticalLineTo(22f)
      curveTo(48f, 23.657f, 46.657f, 25f, 45f, 25f)
      close()
    }
    path(fill = SolidColor(Color(0xFFFFE691))) {
      moveTo(32f, 5f)
      moveToRelative(-5f, 0f)
      arcToRelative(5f, 5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 10f, 0f)
      arcToRelative(5f, 5f, 0f, isMoreThanHalf = true, isPositiveArc = true, -10f, 0f)
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(53f, 41f)
      curveToRelative(0f, 0f, 0f, -14f, 0f, -15f)
      curveToRelative(-2.449f, 0.323f, -5f, 2.508f, -5f, 5.046f)
      verticalLineTo(41f)
      curveToRelative(0f, 0.552f, -0.449f, 1f, -1f, 1f)
      horizontalLineToRelative(-8f)
      curveToRelative(-2.761f, 0f, -5f, 2.238f, -5f, 5f)
      horizontalLineToRelative(13f)
      curveTo(50.314f, 47f, 53f, 44.313f, 53f, 41f)
      close()
    }
    path(fill = SolidColor(Color.White)) {
      moveTo(18.528f, 13.753f)
      curveToRelative(-0.271f, 0f, -0.546f, -0.073f, -0.793f, -0.228f)
      curveToRelative(-0.703f, -0.439f, -0.916f, -1.365f, -0.477f, -2.067f)
      lineToRelative(1.502f, -2.403f)
      curveTo(19.955f, 7.142f, 22.015f, 6f, 24.271f, 6f)
      horizontalLineTo(24.5f)
      curveTo(25.329f, 6f, 26f, 6.672f, 26f, 7.5f)
      reflectiveCurveTo(25.329f, 9f, 24.5f, 9f)
      horizontalLineToRelative(-0.229f)
      curveToRelative(-1.215f, 0f, -2.324f, 0.615f, -2.968f, 1.645f)
      lineToRelative(-1.502f, 2.403f)
      curveTo(19.517f, 13.503f, 19.028f, 13.753f, 18.528f, 13.753f)
      close()
    }
  }.build()
}

// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Theme.Suitcase: ImageVector by lazy(LazyThreadSafetyMode.PUBLICATION) {
  ImageVector.Builder(
    name = "Theme.Suitcase",
    defaultWidth = 64.dp,
    defaultHeight = 64.dp,
    viewportWidth = 64f,
    viewportHeight = 64f,
  ).apply {
    path(fill = SolidColor(Color(0xFFBD6300))) {
      moveTo(43f, 22f)
      horizontalLineTo(21f)
      verticalLineToRelative(-9f)
      curveToRelative(0f, -2.206f, 1.794f, -4f, 4f, -4f)
      horizontalLineToRelative(14f)
      curveToRelative(2.206f, 0f, 4f, 1.794f, 4f, 4f)
      verticalLineTo(22f)
      close()
      moveTo(25f, 18f)
      horizontalLineToRelative(14f)
      verticalLineToRelative(-5f)
      horizontalLineTo(25f)
      verticalLineTo(18f)
      close()
    }
    path(fill = SolidColor(Color(0xFFDA7200))) {
      moveTo(56f, 21f)
      verticalLineToRelative(26f)
      curveToRelative(0f, 2.76f, -2.24f, 5f, -5f, 5f)
      horizontalLineTo(13f)
      curveToRelative(-2.76f, 0f, -5f, -2.24f, -5f, -5f)
      verticalLineTo(21.01f)
      curveToRelative(0f, -1.39f, 0.56f, -2.64f, 1.46f, -3.54f)
      curveToRelative(0.34f, -0.34f, 0.73f, -0.63f, 1.16f, -0.87f)
      curveToRelative(0.14f, -0.07f, 0.28f, -0.14f, 0.43f, -0.21f)
      curveToRelative(0.3f, -0.12f, 0.62f, -0.22f, 0.94f, -0.29f)
      curveTo(12.32f, 16.04f, 12.66f, 16f, 13f, 16f)
      horizontalLineToRelative(38f)
      curveTo(53.76f, 16f, 56f, 18.24f, 56f, 21f)
      close()
    }
    path(fill = SolidColor(Color(0xFFA0EFFE))) {
      moveTo(41.121f, 28.885f)
      lineToRelative(-7.236f, 7.236f)
      curveToRelative(-1.172f, 1.172f, -3.071f, 1.172f, -4.243f, 0f)
      lineToRelative(-2.828f, -2.828f)
      curveToRelative(-1.172f, -1.172f, -1.172f, -3.071f, 0f, -4.243f)
      lineToRelative(7.236f, -7.236f)
      curveToRelative(1.172f, -1.172f, 3.071f, -1.172f, 4.243f, 0f)
      lineToRelative(2.828f, 2.828f)
      curveTo(42.293f, 25.814f, 42.293f, 27.714f, 41.121f, 28.885f)
      close()
    }
    path(fill = SolidColor(Color(0xFF68E5FD))) {
      moveTo(29.316f, 30.571f)
      lineToRelative(6.276f, -6.277f)
      curveToRelative(0.421f, -0.421f, 1.103f, -0.421f, 1.524f, 0f)
      lineToRelative(1.525f, 1.525f)
      curveToRelative(0.421f, 0.421f, 0.421f, 1.104f, 0f, 1.525f)
      lineToRelative(-6.276f, 6.277f)
      curveToRelative(-0.421f, 0.421f, -1.104f, 0.421f, -1.525f, 0f)
      lineToRelative(-1.525f, -1.525f)
      curveTo(28.895f, 31.674f, 28.895f, 30.992f, 29.316f, 30.571f)
      close()
    }
    path(fill = SolidColor(Color(0xFF68E5FD))) {
      moveTo(42f, 40f)
      moveToRelative(-5f, 0f)
      arcToRelative(5f, 5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 10f, 0f)
      arcToRelative(5f, 5f, 0f, isMoreThanHalf = true, isPositiveArc = true, -10f, 0f)
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
    path(fill = SolidColor(Color(0xFFFFCE29))) {
      moveTo(45f, 16f)
      horizontalLineToRelative(6f)
      verticalLineToRelative(36f)
      horizontalLineToRelative(-6f)
      close()
    }
    path(fill = SolidColor(Color(0xFFFFCE29))) {
      moveTo(13f, 16f)
      horizontalLineToRelative(6f)
      verticalLineToRelative(36f)
      horizontalLineToRelative(-6f)
      close()
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(55.509f, 25.023f)
      curveTo(52.91f, 25.271f, 51f, 27.603f, 51f, 30.214f)
      verticalLineTo(45f)
      curveToRelative(0f, 1.105f, -0.895f, 2f, -2f, 2f)
      horizontalLineTo(25.997f)
      curveTo(23.235f, 47f, 21f, 49.239f, 21f, 52f)
      horizontalLineToRelative(30f)
      curveToRelative(2.761f, 0f, 5f, -2.239f, 5f, -5f)
      verticalLineTo(25.003f)
      curveTo(55.838f, 25.003f, 55.675f, 25.008f, 55.509f, 25.023f)
      close()
    }
    path(
      fill = SolidColor(Color.White),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(43f, 16f)
      curveToRelative(0f, 2.77f, -2.24f, 5.01f, -5f, 5.01f)
      horizontalLineTo(15f)
      curveToRelative(-1.1f, 0f, -2f, 0.89f, -2f, 2f)
      verticalLineToRelative(13.98f)
      curveTo(13f, 39.76f, 10.76f, 42f, 8f, 42f)
      verticalLineTo(21.01f)
      curveToRelative(0f, -1.39f, 0.56f, -2.64f, 1.46f, -3.54f)
      curveToRelative(0.34f, -0.34f, 0.73f, -0.63f, 1.16f, -0.87f)
      curveToRelative(0.14f, -0.07f, 0.28f, -0.14f, 0.43f, -0.21f)
      curveToRelative(0.3f, -0.12f, 0.62f, -0.22f, 0.94f, -0.29f)
      curveTo(12.32f, 16.04f, 12.66f, 16f, 13f, 16f)
      horizontalLineTo(43f)
      close()
    }
    path(fill = SolidColor(Color.White)) {
      moveTo(12f, 26f)
      curveToRelative(-0.828f, 0f, -1.5f, -0.671f, -1.5f, -1.5f)
      verticalLineToRelative(-3.466f)
      curveToRelative(0f, -1.378f, 1.121f, -2.5f, 2.5f, -2.5f)
      horizontalLineToRelative(6.5f)
      curveToRelative(0.828f, 0f, 1.5f, 0.671f, 1.5f, 1.5f)
      reflectiveCurveToRelative(-0.672f, 1.5f, -1.5f, 1.5f)
      horizontalLineToRelative(-6f)
      verticalLineTo(24.5f)
      curveTo(13.5f, 25.329f, 12f, 26f, 12f, 26f)
      close()
    }
  }.build()
}

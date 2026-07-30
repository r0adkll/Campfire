// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Theme.Globe: ImageVector by lazy(LazyThreadSafetyMode.PUBLICATION) {
  ImageVector.Builder(
    name = "Theme.Globe",
    defaultWidth = 64.dp,
    defaultHeight = 64.dp,
    viewportWidth = 64f,
    viewportHeight = 64f,
  ).apply {
    path(fill = SolidColor(Color(0xFF68E5FD))) {
      moveTo(53.17f, 41f)
      curveToRelative(-0.44f, 1.03f, -0.95f, 2.03f, -1.55f, 3f)
      curveToRelative(-0.92f, 1.52f, -2.04f, 2.95f, -3.36f, 4.26f)
      curveToRelative(-1.42f, 1.42f, -2.98f, 2.62f, -4.63f, 3.58f)
      curveToRelative(-2.39f, 1.41f, -4.98f, 2.34f, -7.63f, 2.8f)
      curveToRelative(-2.64f, 0.48f, -5.36f, 0.48f, -8f, 0f)
      curveToRelative(-1.48f, -0.26f, -2.95f, -0.67f, -4.37f, -1.22f)
      curveToRelative(-2.88f, -1.11f, -5.57f, -2.83f, -7.89f, -5.16f)
      curveToRelative(-1.32f, -1.31f, -2.44f, -2.74f, -3.36f, -4.26f)
      curveToRelative(-0.6f, -0.97f, -1.11f, -1.97f, -1.55f, -3f)
      curveToRelative(-2.44f, -5.73f, -2.44f, -12.27f, 0f, -18f)
      curveToRelative(0.44f, -1.03f, 0.95f, -2.03f, 1.55f, -3f)
      curveToRelative(0.92f, -1.52f, 2.04f, -2.95f, 3.36f, -4.26f)
      curveToRelative(3.09f, -3.09f, 6.84f, -5.12f, 10.79f, -6.08f)
      curveToRelative(4.85f, -1.19f, 10.02f, -0.77f, 14.65f, 1.25f)
      curveToRelative(2.57f, 1.11f, 4.98f, 2.72f, 7.08f, 4.83f)
      curveToRelative(1.32f, 1.31f, 2.44f, 2.74f, 3.36f, 4.26f)
      curveToRelative(0.6f, 0.97f, 1.11f, 1.97f, 1.55f, 3f)
      curveTo(55.61f, 28.73f, 55.61f, 35.27f, 53.17f, 41f)
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
    path(fill = SolidColor(Color(0xFF37D0EE))) {
      moveTo(48.26f, 48.26f)
      curveToRelative(-1.42f, 1.42f, -2.98f, 2.62f, -4.63f, 3.58f)
      lineToRelative(-1.19f, -10.07f)
      curveTo(42.32f, 40.76f, 41.47f, 40f, 40.46f, 40f)
      horizontalLineTo(39f)
      curveToRelative(-1.1f, 0f, -2f, -0.9f, -2f, -2f)
      verticalLineToRelative(-1f)
      curveToRelative(0f, -1.1f, 0.9f, -2f, 2f, -2f)
      horizontalLineToRelative(11f)
      verticalLineToRelative(-9f)
      curveToRelative(0f, -0.55f, -0.45f, -1f, -1f, -1f)
      horizontalLineToRelative(-2f)
      curveToRelative(-0.55f, 0f, -1f, 0.45f, -1f, 1f)
      verticalLineToRelative(4f)
      curveToRelative(0f, 1.1f, -0.9f, 2f, -2f, 2f)
      horizontalLineToRelative(-6f)
      curveToRelative(-1.1f, 0f, -2f, -0.9f, -2f, -2f)
      verticalLineToRelative(-1f)
      curveToRelative(0f, -1.1f, 0.9f, -2f, 2f, -2f)
      horizontalLineToRelative(2f)
      curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
      verticalLineToRelative(-1f)
      curveToRelative(0f, -0.55f, -0.45f, -1f, -1f, -1f)
      reflectiveCurveToRelative(-1.05f, -0.22f, -1.41f, -0.59f)
      curveTo(38.22f, 23.05f, 38f, 22.55f, 38f, 22f)
      curveToRelative(0f, -1.1f, 0.9f, -2f, 2f, -2f)
      horizontalLineToRelative(5.2f)
      curveToRelative(0.95f, 0f, 1.25f, -1.28f, 0.4f, -1.7f)
      lineToRelative(-3.71f, -1.85f)
      curveToRelative(-0.58f, -0.29f, -1.05f, -0.76f, -1.34f, -1.34f)
      lineToRelative(-0.66f, -1.32f)
      curveToRelative(-0.49f, -0.99f, -0.09f, -2.19f, 0.9f, -2.68f)
      lineToRelative(0.39f, -0.2f)
      curveToRelative(2.57f, 1.11f, 4.98f, 2.72f, 7.08f, 4.83f)
      curveTo(57.25f, 24.72f, 57.25f, 39.28f, 48.26f, 48.26f)
      close()
    }
    path(fill = SolidColor(Color(0xFF37D0EE))) {
      moveTo(29f, 22f)
      verticalLineToRelative(4f)
      curveToRelative(0f, 0.55f, -0.22f, 1.05f, -0.59f, 1.41f)
      curveTo(28.05f, 27.78f, 27.55f, 28f, 27f, 28f)
      curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
      verticalLineToRelative(3f)
      curveToRelative(0f, 1.1f, -0.9f, 2f, -2f, 2f)
      horizontalLineToRelative(-1f)
      curveToRelative(-1.1f, 0f, -2f, -0.9f, -2f, -2f)
      verticalLineToRelative(-1f)
      curveToRelative(0f, -0.55f, -0.22f, -1.05f, -0.59f, -1.41f)
      curveTo(19.05f, 30.22f, 18.55f, 30f, 18f, 30f)
      curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
      verticalLineToRelative(5f)
      curveToRelative(0f, 1.66f, 1.34f, 3f, 3f, 3f)
      horizontalLineToRelative(3.42f)
      curveToRelative(1.87f, 0f, 3.28f, 1.68f, 2.96f, 3.52f)
      lineToRelative(-1.75f, 9.9f)
      curveToRelative(-2.88f, -1.11f, -5.57f, -2.83f, -7.89f, -5.16f)
      curveToRelative(-8.99f, -8.98f, -8.99f, -23.54f, 0f, -32.52f)
      curveToRelative(3.09f, -3.09f, 6.84f, -5.12f, 10.79f, -6.08f)
      curveToRelative(0.27f, 0.17f, 0.5f, 0.41f, 0.66f, 0.72f)
      curveToRelative(0.45f, 0.89f, 0.08f, 1.98f, -0.81f, 2.43f)
      lineToRelative(-2.72f, 1.36f)
      curveTo(22.64f, 14.68f, 22f, 15.72f, 22f, 16.85f)
      verticalLineTo(18f)
      curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
      horizontalLineToRelative(3f)
      curveTo(28.1f, 20f, 29f, 20.9f, 29f, 22f)
      close()
    }
    path(fill = SolidColor(Color(0xFF008AA9))) {
      moveTo(32f, 9f)
      curveToRelative(-7.535f, 0f, -13.438f, 10.103f, -13.438f, 23f)
      reflectiveCurveTo(24.465f, 55f, 32f, 55f)
      reflectiveCurveToRelative(13.437f, -10.103f, 13.437f, -23f)
      reflectiveCurveTo(39.535f, 9f, 32f, 9f)
      close()
      moveTo(21.562f, 32f)
      curveToRelative(0f, -9.865f, 3.962f, -18.313f, 8.938f, -19.758f)
      verticalLineToRelative(39.517f)
      curveTo(25.524f, 50.313f, 21.562f, 41.865f, 21.562f, 32f)
      close()
      moveTo(33.5f, 51.758f)
      verticalLineTo(12.242f)
      curveToRelative(4.976f, 1.445f, 8.937f, 9.893f, 8.937f, 19.758f)
      reflectiveCurveTo(38.477f, 50.313f, 33.5f, 51.758f)
      close()
    }
    path(fill = SolidColor(Color(0xFF008AA9))) {
      moveTo(54.928f, 30.5f)
      horizontalLineTo(9.072f)
      curveToRelative(-0.065f, 1f, -0.065f, 2f, 0f, 3f)
      horizontalLineToRelative(45.857f)
      curveTo(54.993f, 32.5f, 54.993f, 31.5f, 54.928f, 30.5f)
      close()
    }
    path(fill = SolidColor(Color(0xFF008AA9))) {
      moveTo(53.17f, 23f)
      horizontalLineTo(10.83f)
      curveToRelative(0.44f, -1.03f, 0.95f, -2.03f, 1.55f, -3f)
      horizontalLineToRelative(39.24f)
      curveTo(52.22f, 20.97f, 52.73f, 21.97f, 53.17f, 23f)
      close()
    }
    path(fill = SolidColor(Color(0xFF008AA9))) {
      moveTo(53.17f, 41f)
      curveToRelative(-0.44f, 1.03f, -0.95f, 2.03f, -1.55f, 3f)
      horizontalLineTo(12.38f)
      curveToRelative(-0.6f, -0.97f, -1.11f, -1.97f, -1.55f, -3f)
      horizontalLineTo(53.17f)
      close()
    }
    path(fill = SolidColor(Color.White)) {
      moveTo(15.045f, 24.927f)
      curveToRelative(-0.229f, 0f, -0.46f, -0.053f, -0.678f, -0.163f)
      curveToRelative(-0.739f, -0.375f, -1.033f, -1.278f, -0.658f, -2.017f)
      curveToRelative(2.019f, -3.977f, 5.339f, -7.241f, 9.347f, -9.192f)
      curveToRelative(0.743f, -0.364f, 1.642f, -0.053f, 2.005f, 0.692f)
      curveToRelative(0.363f, 0.745f, 0.053f, 1.643f, -0.692f, 2.005f)
      curveToRelative(-3.424f, 1.667f, -6.26f, 4.456f, -7.985f, 7.853f)
      curveTo(16.119f, 24.627f, 15.592f, 24.927f, 15.045f, 24.927f)
      close()
    }
    path(
      fill = SolidColor(Color.White),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(32f, 14f)
      curveToRelative(2.577f, 0f, 4.674f, -1.957f, 4.946f, -4.461f)
      curveTo(35.352f, 9.19f, 33.699f, 9f, 32f, 9f)
      curveTo(19.297f, 9f, 9f, 19.297f, 9f, 32f)
      curveToRelative(0f, 1.699f, 0.19f, 3.352f, 0.539f, 4.946f)
      curveTo(12.044f, 36.674f, 14f, 34.577f, 14f, 32f)
      curveTo(14f, 22.075f, 22.075f, 14f, 32f, 14f)
      close()
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(54.461f, 27.054f)
      curveTo(51.956f, 27.326f, 50f, 29.423f, 50f, 32f)
      curveToRelative(0f, 9.925f, -8.075f, 18f, -18f, 18f)
      curveToRelative(-2.577f, 0f, -4.674f, 1.957f, -4.946f, 4.461f)
      curveTo(28.648f, 54.81f, 30.301f, 55f, 32f, 55f)
      curveToRelative(12.703f, 0f, 23f, -10.297f, 23f, -23f)
      curveTo(55f, 30.301f, 54.81f, 28.648f, 54.461f, 27.054f)
      close()
    }
    path(fill = SolidColor(Color(0xFFFD3C4F))) {
      moveTo(26f, 34f)
      horizontalLineTo(7f)
      curveToRelative(-1.104f, 0f, -2f, -0.896f, -2f, -2f)
      reflectiveCurveToRelative(0.896f, -2f, 2f, -2f)
      horizontalLineToRelative(19f)
      curveToRelative(1.104f, 0f, 2f, 0.896f, 2f, 2f)
      reflectiveCurveTo(27.104f, 34f, 26f, 34f)
      close()
    }
    path(fill = SolidColor(Color(0xFFFD3C4F))) {
      moveTo(22.477f, 28.128f)
      verticalLineToRelative(7.745f)
      curveToRelative(0f, 1.27f, 1.313f, 2.049f, 2.333f, 1.385f)
      lineToRelative(5.962f, -3.88f)
      curveToRelative(1.002f, -0.652f, 1.001f, -2.12f, -0.002f, -2.771f)
      lineToRelative(-5.962f, -3.866f)
      curveTo(23.788f, 26.08f, 22.477f, 26.859f, 22.477f, 28.128f)
      close()
    }
  }.build()
}

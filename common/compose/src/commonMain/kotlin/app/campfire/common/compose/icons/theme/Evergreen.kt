// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Theme.Evergreen: ImageVector by lazy(LazyThreadSafetyMode.PUBLICATION) {
  ImageVector.Builder(
    name = "Theme.Evergreen",
    defaultWidth = 64.dp,
    defaultHeight = 64.dp,
    viewportWidth = 64f,
    viewportHeight = 64f,
  ).apply {
    path(fill = SolidColor(Color(0xFFBD6300))) {
      moveTo(34f, 54f)
      horizontalLineToRelative(-4.001f)
      curveToRelative(-1.657f, 0f, -3f, -1.343f, -3f, -3f)
      verticalLineTo(29.001f)
      horizontalLineTo(37f)
      verticalLineTo(51f)
      curveTo(37f, 52.657f, 35.657f, 54f, 34f, 54f)
      close()
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(19f, 61f)
      arcToRelative(13f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, 26f, 0f)
      arcToRelative(13f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, -26f, 0f)
      close()
    }
    path(fill = SolidColor(Color(0xFF98C900))) {
      moveTo(29.342f, 5.283f)
      lineTo(18.619f, 18.53f)
      curveTo(17.167f, 20.324f, 18.443f, 23f, 20.751f, 23f)
      horizontalLineToRelative(22.497f)
      curveToRelative(2.308f, 0f, 3.585f, -2.676f, 2.133f, -4.47f)
      lineTo(34.658f, 5.283f)
      curveTo(33.289f, 3.593f, 30.711f, 3.593f, 29.342f, 5.283f)
      close()
    }
    path(fill = SolidColor(Color(0xFF98C900))) {
      moveTo(32f, 14f)
      lineTo(16.038f, 29.237f)
      curveTo(13.855f, 31.32f, 15.33f, 35f, 18.347f, 35f)
      horizontalLineToRelative(27.306f)
      curveToRelative(3.017f, 0f, 4.492f, -3.68f, 2.309f, -5.763f)
      lineTo(32f, 14f)
      close()
    }
    path(fill = SolidColor(Color(0xFF98C900))) {
      moveTo(32f, 22f)
      lineTo(13.143f, 39.748f)
      curveTo(10.757f, 41.993f, 12.347f, 46f, 15.623f, 46f)
      horizontalLineToRelative(32.754f)
      curveToRelative(3.276f, 0f, 4.865f, -4.007f, 2.48f, -6.252f)
      lineTo(32f, 22f)
      close()
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(48.38f, 46f)
      horizontalLineTo(37f)
      verticalLineToRelative(5f)
      curveToRelative(0f, 1.66f, -1.34f, 3f, -3f, 3f)
      horizontalLineToRelative(-4f)
      curveToRelative(-0.74f, 0f, -1.42f, -0.27f, -1.94f, -0.72f)
      curveToRelative(0.29f, -2.09f, 1.89f, -3.77f, 3.94f, -4.18f)
      verticalLineTo(46f)
      curveToRelative(0f, -2.76f, 2.24f, -5f, 5f, -5f)
      horizontalLineToRelative(7.89f)
      lineToRelative(-2.52f, -2.37f)
      curveToRelative(-1.46f, -1.38f, -1.96f, -3.5f, -1.25f, -5.39f)
      curveToRelative(0.39f, -1.07f, 1.13f, -1.94f, 2.07f, -2.51f)
      curveToRelative(-0.52f, -1.68f, -0.14f, -3.58f, 1.16f, -4.94f)
      lineToRelative(3.61f, 3.45f)
      curveToRelative(2.15f, 2.04f, 0.76f, 5.63f, -2.16f, 5.75f)
      lineToRelative(5.06f, 4.76f)
      curveTo(53.24f, 41.99f, 51.65f, 46f, 48.38f, 46f)
      close()
    }
    path(
      fill = SolidColor(Color.White),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(33.23f, 8.43f)
      lineToRelative(-8.25f, 10.19f)
      curveToRelative(0.99f, 0.54f, 1.79f, 1.42f, 2.23f, 2.52f)
      curveToRelative(0.76f, 1.9f, 0.29f, 4.07f, -1.19f, 5.48f)
      lineToRelative(-6.53f, 6.23f)
      curveToRelative(-0.93f, 0.89f, -2.1f, 1.35f, -3.28f, 1.38f)
      curveToRelative(-1.42f, -1.18f, -1.74f, -3.5f, -0.17f, -4.99f)
      lineTo(22.57f, 23f)
      horizontalLineToRelative(-1.82f)
      curveToRelative(-2.31f, 0f, -3.58f, -2.68f, -2.13f, -4.47f)
      lineTo(29.34f, 5.28f)
      curveToRelative(1.26f, -1.56f, 3.56f, -1.68f, 4.98f, -0.36f)
      curveTo(34.42f, 6.14f, 34.06f, 7.4f, 33.23f, 8.43f)
      close()
    }
    path(fill = SolidColor(Color.White)) {
      moveTo(26.499f, 16f)
      curveToRelative(-0.328f, 0f, -0.659f, -0.107f, -0.937f, -0.329f)
      curveToRelative(-0.646f, -0.518f, -0.751f, -1.461f, -0.233f, -2.108f)
      lineToRelative(4f, -5f)
      curveToRelative(0.517f, -0.646f, 1.458f, -0.752f, 2.108f, -0.233f)
      curveToRelative(0.646f, 0.518f, 0.751f, 1.461f, 0.233f, 2.108f)
      lineToRelative(-4f, 5f)
      curveTo(27.375f, 15.808f, 26.939f, 16f, 26.499f, 16f)
      close()
    }
  }.build()
}

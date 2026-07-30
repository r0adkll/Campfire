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

val CampfireIcons.Theme.Star: ImageVector by lazy(LazyThreadSafetyMode.PUBLICATION) {
  ImageVector.Builder(
    name = "Theme.Star",
    defaultWidth = 64.dp,
    defaultHeight = 64.dp,
    viewportWidth = 64f,
    viewportHeight = 64f,
  ).apply {
    path(fill = SolidColor(Color(0xFFFFCE29))) {
      moveTo(32f, 50.165f)
      lineToRelative(-12.123f, 6.374f)
      curveToRelative(-2.887f, 1.518f, -6.262f, -0.934f, -5.711f, -4.149f)
      lineToRelative(2.315f, -13.5f)
      lineTo(6.673f, 29.33f)
      curveToRelative(-2.336f, -2.277f, -1.047f, -6.244f, 2.181f, -6.713f)
      lineToRelative(13.554f, -1.97f)
      lineToRelative(6.062f, -12.282f)
      curveToRelative(1.444f, -2.925f, 5.615f, -2.925f, 7.059f, 0f)
      lineToRelative(6.062f, 12.282f)
      lineToRelative(13.554f, 1.97f)
      curveToRelative(3.228f, 0.469f, 4.517f, 4.436f, 2.181f, 6.713f)
      lineToRelative(-9.808f, 9.561f)
      lineToRelative(2.315f, 13.5f)
      curveToRelative(0.551f, 3.215f, -2.823f, 5.667f, -5.711f, 4.149f)
      lineTo(32f, 50.165f)
      close()
    }
    path(
      stroke = SolidColor(Color.White),
      strokeLineWidth = 3f,
      strokeLineCap = StrokeCap.Round,
      strokeLineJoin = StrokeJoin.Round,
    ) {
      moveTo(28.353f, 17.643f)
      lineTo(31.115f, 12.047f)
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
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(53.837f, 25.75f)
      lineToRelative(-9.809f, 9.561f)
      curveToRelative(-1.179f, 1.149f, -1.716f, 2.804f, -1.438f, 4.426f)
      lineToRelative(1.95f, 11.375f)
      lineTo(34.327f, 45.74f)
      curveToRelative(-1.457f, -0.766f, -3.197f, -0.766f, -4.654f, 0f)
      lineTo(17.55f, 52.113f)
      curveToRelative(-1.382f, 0.727f, -2.261f, 2.015f, -2.551f, 3.435f)
      curveToRelative(1.113f, 1.346f, 3.088f, 1.931f, 4.877f, 0.991f)
      lineTo(32f, 50.165f)
      lineToRelative(12.123f, 6.374f)
      curveToRelative(2.887f, 1.518f, 6.262f, -0.934f, 5.711f, -4.149f)
      lineToRelative(-2.315f, -13.5f)
      lineToRelative(9.808f, -9.561f)
      curveToRelative(1.448f, -1.411f, 1.501f, -3.47f, 0.565f, -4.945f)
      curveTo(56.452f, 24.222f, 54.956f, 24.66f, 53.837f, 25.75f)
      close()
    }
    path(
      fill = SolidColor(Color.White),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(28.471f, 8.365f)
      lineToRelative(-6.062f, 12.282f)
      lineToRelative(-13.554f, 1.97f)
      curveToRelative(-2.006f, 0.292f, -3.262f, 1.934f, -3.365f, 3.683f)
      curveToRelative(0.9f, 0.821f, 2.084f, 1.318f, 3.36f, 1.318f)
      curveToRelative(0.239f, 0f, 0.481f, -0.017f, 0.725f, -0.052f)
      lineToRelative(13.555f, -1.97f)
      curveToRelative(1.629f, -0.237f, 3.037f, -1.26f, 3.765f, -2.735f)
      lineToRelative(6.062f, -12.282f)
      curveToRelative(0.692f, -1.401f, 0.646f, -2.96f, 0.045f, -4.278f)
      curveTo(31.308f, 5.865f, 29.366f, 6.551f, 28.471f, 8.365f)
      close()
    }
  }.build()
}

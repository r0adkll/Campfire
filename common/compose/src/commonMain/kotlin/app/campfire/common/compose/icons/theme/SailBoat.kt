// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Theme.SailBoat: ImageVector by lazy(LazyThreadSafetyMode.PUBLICATION) {
  ImageVector.Builder(
    name = "Theme.SailBoat",
    defaultWidth = 64.dp,
    defaultHeight = 64.dp,
    viewportWidth = 64f,
    viewportHeight = 64f,
  ).apply {
    path(fill = SolidColor(Color(0xFFDA7200))) {
      moveTo(31.5f, 5f)
      lineTo(31.5f, 5f)
      curveTo(30.672f, 5f, 30f, 5.672f, 30f, 6.5f)
      verticalLineTo(31f)
      horizontalLineToRelative(3f)
      verticalLineTo(6.5f)
      curveTo(33f, 5.672f, 32.328f, 5f, 31.5f, 5f)
      close()
    }
    path(fill = SolidColor(Color(0xFFFD3C4F))) {
      moveTo(42f, 46f)
      horizontalLineTo(25f)
      lineToRelative(-11.707f, -9.198f)
      curveTo(11.845f, 35.664f, 11f, 33.925f, 11f, 32.084f)
      verticalLineToRelative(0f)
      curveTo(11f, 30.933f, 11.933f, 30f, 13.084f, 30f)
      horizontalLineToRelative(39.3f)
      curveToRelative(1.751f, 0f, 2.657f, 2.091f, 1.459f, 3.368f)
      lineTo(42f, 46f)
      close()
    }
    path(fill = SolidColor(Color(0xFF37D0EE))) {
      moveTo(52.978f, 40f)
      curveToRelative(-2.612f, 0f, -3.826f, 2f, -6.649f, 2f)
      curveToRelative(-1.403f, 0f, -2.044f, -0.357f, -2.855f, -0.81f)
      curveToRelative(-2.236f, -1.565f, -6.41f, -1.557f, -8.631f, 0f)
      curveTo(34.002f, 41.659f, 33.393f, 41.998f, 32f, 42f)
      curveToRelative(-1.393f, -0.002f, -2.002f, -0.341f, -2.842f, -0.809f)
      curveToRelative(-2.231f, -1.562f, -6.399f, -1.562f, -8.631f, 0f)
      curveTo(19.716f, 41.642f, 19.075f, 42f, 17.672f, 42f)
      curveToRelative(-2.824f, 0f, -4.037f, -2f, -6.649f, -2f)
      curveTo(8.924f, 39.999f, 9f, 42.084f, 9f, 42.084f)
      lineTo(8.999f, 49f)
      curveToRelative(0f, 2.209f, 1.791f, 4f, 4f, 4f)
      lineToRelative(38f, 0.001f)
      curveToRelative(2.209f, 0f, 4f, -1.791f, 4f, -4f)
      lineTo(55f, 42.085f)
      curveTo(55f, 42.085f, 55.076f, 40.001f, 52.978f, 40f)
      close()
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(54.999f, 49.001f)
      lineTo(55f, 42.085f)
      curveToRelative(0f, 0f, 0.001f, -0.033f, -0.001f, -0.086f)
      curveToRelative(-2.76f, 0f, -4.998f, 2.238f, -4.999f, 4.997f)
      verticalLineTo(48f)
      lineToRelative(-11f, -0.001f)
      curveToRelative(-2.761f, 0f, -5f, 2.238f, -5f, 5f)
      curveTo(34f, 53f, 34f, 53f, 34f, 53f)
      lineToRelative(16.999f, 0f)
      curveTo(53.208f, 53f, 54.999f, 51.21f, 54.999f, 49.001f)
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
      moveTo(33f, 7f)
      verticalLineToRelative(19f)
      lineToRelative(12.719f, -0.748f)
      curveToRelative(1.696f, -0.1f, 2.503f, -2.135f, 1.337f, -3.37f)
      lineTo(33f, 7f)
      close()
    }
    path(fill = SolidColor(Color(0xFF37D0EE))) {
      moveTo(28f, 26f)
      verticalLineTo(7f)
      lineTo(17.028f, 17.972f)
      curveToRelative(-0.984f, 0.984f, -0.687f, 2.647f, 0.576f, 3.23f)
      lineTo(28f, 26f)
      close()
    }
    path(
      fill = SolidColor(Color.White),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(28f, 7f)
      lineTo(17.028f, 17.972f)
      curveToRelative(-0.694f, 0.694f, -0.74f, 1.72f, -0.269f, 2.473f)
      curveTo(17.464f, 20.8f, 18.228f, 21f, 19f, 21f)
      curveToRelative(1.279f, 0f, 2.56f, -0.488f, 3.535f, -1.465f)
      lineTo(28f, 14.07f)
      verticalLineTo(7f)
      close()
    }
    path(fill = SolidColor(Color(0xFFFFE691))) {
      moveTo(53.25f, 34f)
      horizontalLineTo(11.319f)
      curveToRelative(0.369f, 1.093f, 1.046f, 2.072f, 1.974f, 2.802f)
      lineTo(13.545f, 37f)
      horizontalLineToRelative(36.892f)
      lineTo(53.25f, 34f)
      close()
    }
    path(fill = SolidColor(Color.White)) {
      moveTo(22.5f, 19f)
      curveToRelative(-0.384f, 0f, -0.768f, -0.146f, -1.061f, -0.439f)
      curveToRelative(-0.586f, -0.586f, -0.586f, -1.535f, 0f, -2.121f)
      lineToRelative(3f, -3f)
      curveToRelative(0.586f, -0.586f, 1.535f, -0.586f, 2.121f, 0f)
      reflectiveCurveToRelative(0.586f, 1.535f, 0f, 2.121f)
      lineToRelative(-3f, 3f)
      curveTo(23.268f, 18.854f, 22.884f, 19f, 22.5f, 19f)
      close()
    }
  }.build()
}

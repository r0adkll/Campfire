package app.campfire.common.compose.icons.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Theme.PaperMap: ImageVector by lazy(LazyThreadSafetyMode.PUBLICATION) {
  ImageVector.Builder(
    name = "Theme.PaperMap",
    defaultWidth = 64.dp,
    defaultHeight = 64.dp,
    viewportWidth = 64f,
    viewportHeight = 64f,
  ).apply {
    path(fill = SolidColor(Color(0xFFFFA500))) {
      moveTo(37.682f, 13.663f)
      lineToRelative(-12.349f, 2.906f)
      curveToRelative(-1.205f, 0.284f, -2.46f, 0.284f, -3.665f, 0f)
      lineToRelative(-10.385f, -2.444f)
      curveTo(8.84f, 13.551f, 6.5f, 15.404f, 6.5f, 17.913f)
      verticalLineToRelative(28.711f)
      curveToRelative(0f, 1.392f, 0.958f, 2.601f, 2.313f, 2.92f)
      lineToRelative(11.869f, 2.793f)
      curveToRelative(1.853f, 0.436f, 3.783f, 0.436f, 5.636f, 0f)
      lineToRelative(12.349f, -2.906f)
      curveToRelative(1.205f, -0.284f, 2.46f, -0.284f, 3.665f, 0f)
      lineToRelative(10.385f, 2.444f)
      curveToRelative(2.443f, 0.575f, 4.783f, -1.279f, 4.783f, -3.788f)
      verticalLineTo(19.376f)
      curveToRelative(0f, -1.392f, -0.958f, -2.601f, -2.313f, -2.92f)
      lineToRelative(-11.869f, -2.793f)
      curveTo(41.465f, 13.227f, 39.535f, 13.227f, 37.682f, 13.663f)
      close()
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(8.75f, 61f)
      arcToRelative(23.25f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, 46.5f, 0f)
      arcToRelative(23.25f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, -46.5f, 0f)
      close()
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(37.682f, 13.663f)
      lineToRelative(-12.349f, 2.906f)
      curveToRelative(-0.603f, 0.142f, -1.217f, 0.213f, -1.832f, 0.213f)
      verticalLineToRelative(35.882f)
      curveToRelative(0.946f, 0f, 1.891f, -0.109f, 2.818f, -0.327f)
      lineToRelative(12.349f, -2.906f)
      curveToRelative(0.603f, -0.142f, 1.217f, -0.213f, 1.832f, -0.213f)
      verticalLineTo(13.336f)
      curveTo(39.554f, 13.336f, 38.609f, 13.445f, 37.682f, 13.663f)
      close()
    }
    path(
      fill = SolidColor(Color.White),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(21.668f, 16.569f)
      lineToRelative(-10.385f, -2.444f)
      curveTo(8.84f, 13.551f, 6.5f, 15.404f, 6.5f, 17.913f)
      verticalLineTo(28f)
      curveToRelative(2.762f, 0f, 5f, -2.239f, 5f, -5f)
      verticalLineToRelative(-3.687f)
      lineToRelative(5.778f, 1.36f)
      curveToRelative(2.691f, 0.629f, 5.38f, -1.034f, 6.013f, -3.722f)
      curveToRelative(0.014f, -0.06f, 0.012f, -0.12f, 0.024f, -0.18f)
      curveTo(22.762f, 16.758f, 22.21f, 16.696f, 21.668f, 16.569f)
      close()
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(52.5f, 34f)
      verticalLineToRelative(11.359f)
      curveToRelative(0f, 0.654f, -0.618f, 1.132f, -1.251f, 0.968f)
      lineToRelative(-4.492f, -1.167f)
      curveToRelative(-2.808f, -0.729f, -5.705f, 1.093f, -6.193f, 4.036f)
      curveToRelative(0.009f, 0.011f, 0.013f, 0.015f, 0.022f, 0.027f)
      curveToRelative(0.586f, 0.006f, 1.172f, 0.073f, 1.746f, 0.208f)
      lineToRelative(10.385f, 2.444f)
      curveToRelative(2.443f, 0.575f, 4.783f, -1.279f, 4.783f, -3.788f)
      verticalLineTo(29f)
      curveTo(54.738f, 29f, 52.5f, 31.239f, 52.5f, 34f)
      close()
    }
    path(
      stroke = SolidColor(Color.White),
      strokeLineWidth = 3f,
      strokeLineCap = StrokeCap.Round,
      strokeLineJoin = StrokeJoin.Round,
    ) {
      moveTo(10.5f, 22.5f)
      verticalLineToRelative(-2.924f)
      curveToRelative(0f, -0.645f, 0.601f, -1.121f, 1.229f, -0.973f)
      lineToRelative(3.178f, 0.748f)
    }
  }.build()
}

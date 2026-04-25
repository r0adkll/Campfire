package app.campfire.common.compose.icons.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Theme.Compass: ImageVector by lazy(LazyThreadSafetyMode.PUBLICATION) {
  ImageVector.Builder(
    name = "Theme.Compass",
    defaultWidth = 64.dp,
    defaultHeight = 64.dp,
    viewportWidth = 64f,
    viewportHeight = 64f,
  ).apply {
    path(fill = SolidColor(Color(0xFF9C34C2))) {
      moveTo(32f, 32f)
      moveToRelative(-23f, 0f)
      arcToRelative(23f, 23f, 0f, isMoreThanHalf = true, isPositiveArc = true, 46f, 0f)
      arcToRelative(23f, 23f, 0f, isMoreThanHalf = true, isPositiveArc = true, -46f, 0f)
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
    path(fill = SolidColor(Color(0xFF4CCFF1))) {
      moveTo(32f, 32f)
      moveToRelative(-18f, 0f)
      arcToRelative(18f, 18f, 0f, isMoreThanHalf = true, isPositiveArc = true, 36f, 0f)
      arcToRelative(18f, 18f, 0f, isMoreThanHalf = true, isPositiveArc = true, -36f, 0f)
    }
    path(
      stroke = SolidColor(Color.White),
      strokeLineWidth = 3f,
      strokeLineCap = StrokeCap.Round,
      strokeLineJoin = StrokeJoin.Round,
    ) {
      moveTo(15.047f, 23.427f)
      curveToRelative(1.878f, -3.699f, 4.932f, -6.705f, 8.666f, -8.522f)
    }
    path(fill = SolidColor(Color(0xFF9C34C2))) {
      moveTo(21.136f, 41f)
      curveToRelative(-0.597f, 1.194f, 0.67f, 2.461f, 1.864f, 1.864f)
      lineToRelative(7.759f, -3.88f)
      curveToRelative(1.78f, -0.89f, 3.391f, -2.056f, 4.78f, -3.445f)
      lineToRelative(-7.078f, -7.078f)
      curveToRelative(-1.389f, 1.389f, -2.555f, 3f, -3.445f, 4.78f)
      lineTo(21.136f, 41f)
      close()
    }
    path(fill = SolidColor(Color(0xFFFD3C4F))) {
      moveTo(38.984f, 30.759f)
      lineTo(42.864f, 23f)
      curveToRelative(0.597f, -1.194f, -0.67f, -2.461f, -1.864f, -1.864f)
      lineToRelative(-7.759f, 3.88f)
      curveToRelative(-1.78f, 0.89f, -3.391f, 2.056f, -4.78f, 3.445f)
      lineToRelative(7.078f, 7.078f)
      curveTo(36.928f, 34.15f, 38.095f, 32.539f, 38.984f, 30.759f)
      close()
    }
    path(fill = SolidColor(Color(0xFFFFA500))) {
      moveTo(32f, 32f)
      moveToRelative(-3f, 0f)
      arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, 6f, 0f)
      arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, -6f, 0f)
    }
    path(fill = SolidColor(Color(0xFF0089AD))) {
      moveTo(32f, 18f)
      moveToRelative(-2f, 0f)
      arcToRelative(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 4f, 0f)
      arcToRelative(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, -4f, 0f)
    }
    path(fill = SolidColor(Color(0xFF0089AD))) {
      moveTo(32f, 46f)
      moveToRelative(-2f, 0f)
      arcToRelative(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 4f, 0f)
      arcToRelative(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, -4f, 0f)
    }
    path(fill = SolidColor(Color(0xFF0089AD))) {
      moveTo(46f, 32f)
      moveToRelative(-2f, 0f)
      arcToRelative(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 4f, 0f)
      arcToRelative(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, -4f, 0f)
    }
    path(fill = SolidColor(Color(0xFF0089AD))) {
      moveTo(18f, 32f)
      moveToRelative(-2f, 0f)
      arcToRelative(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 4f, 0f)
      arcToRelative(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, -4f, 0f)
    }
  }.build()
}

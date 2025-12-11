package app.campfire.common.compose.icons.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.rounded.Path

@Composable
fun rememberWallVectorPainter(
  lightColor: Color = MaterialTheme.colorScheme.primary,
  darkColor: Color = MaterialTheme.colorScheme.primaryContainer,
) = rememberVectorPainter(
  name = "Theme.Wall",
  defaultWidth = 64.dp,
  defaultHeight = 64.dp,
  viewportWidth = 64f,
  viewportHeight = 64f,
  autoMirror = false,
) { w, h ->
  Path(fill = SolidColor(lightColor)) {
    moveTo(44f, 10f)
    horizontalLineTo(20f)
    curveToRelative(-5.523f, 0f, -10f, 4.477f, -10f, 10f)
    verticalLineToRelative(24f)
    curveToRelative(0f, 5.523f, 4.477f, 10f, 10f, 10f)
    horizontalLineToRelative(24f)
    curveToRelative(5.523f, 0f, 10f, -4.477f, 10f, -10f)
    verticalLineTo(20f)
    curveTo(54f, 14.477f, 49.523f, 10f, 44f, 10f)
    close()
  }
  Path(
    fill = SolidColor(Color.Black),
    fillAlpha = 0.3f,
    strokeAlpha = 0.3f,
  ) {
    moveTo(11.875f, 61f)
    arcToRelative(20.125f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, 40.25f, 0f)
    arcToRelative(20.125f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, -40.25f, 0f)
    close()
  }
  Path(fill = SolidColor(darkColor)) {
    moveTo(10f, 22f)
    horizontalLineToRelative(44f)
    verticalLineToRelative(4f)
    horizontalLineToRelative(-44f)
    close()
  }
  Path(fill = SolidColor(darkColor)) {
    moveTo(10f, 38f)
    horizontalLineToRelative(44f)
    verticalLineToRelative(4f)
    horizontalLineToRelative(-44f)
    close()
  }
  Path(fill = SolidColor(darkColor)) {
    moveTo(29f, 24f)
    horizontalLineToRelative(6f)
    verticalLineToRelative(17f)
    horizontalLineToRelative(-6f)
    close()
  }
  Path(fill = SolidColor(darkColor)) {
    moveTo(24f, 10f)
    horizontalLineToRelative(-4.167f)
    curveToRelative(-0.281f, 0f, -0.559f, 0.018f, -0.833f, 0.05f)
    verticalLineTo(23f)
    horizontalLineToRelative(5f)
    verticalLineTo(10f)
    close()
  }
  Path(fill = SolidColor(darkColor)) {
    moveTo(19.833f, 54f)
    horizontalLineTo(24f)
    verticalLineTo(42f)
    horizontalLineToRelative(-5f)
    verticalLineToRelative(11.95f)
    curveTo(19.274f, 53.982f, 19.552f, 54f, 19.833f, 54f)
    close()
  }
  Path(fill = SolidColor(darkColor)) {
    moveTo(45f, 53.95f)
    verticalLineTo(42f)
    horizontalLineToRelative(-5f)
    verticalLineToRelative(12f)
    horizontalLineToRelative(4.167f)
    curveTo(44.448f, 54f, 44.726f, 53.982f, 45f, 53.95f)
    close()
  }
  Path(fill = SolidColor(darkColor)) {
    moveTo(44.167f, 10f)
    horizontalLineTo(40f)
    verticalLineToRelative(13f)
    horizontalLineToRelative(5f)
    verticalLineTo(10.05f)
    curveTo(44.726f, 10.018f, 44.448f, 10f, 44.167f, 10f)
    close()
  }
  Path(
    fill = SolidColor(Color.White),
    fillAlpha = 0.3f,
    strokeAlpha = 0.3f,
  ) {
    moveTo(31f, 15f)
    curveToRelative(2.761f, 0f, 5f, -2.238f, 5f, -5f)
    horizontalLineTo(20f)
    curveToRelative(-5.523f, 0f, -10f, 4.477f, -10f, 10f)
    verticalLineToRelative(22f)
    curveToRelative(2.761f, 0f, 5f, -2.238f, 5f, -5f)
    verticalLineTo(20f)
    curveToRelative(0f, -2.757f, 2.243f, -5f, 5f, -5f)
    horizontalLineTo(31f)
    close()
  }
  Path(
    fill = SolidColor(Color.Black),
    fillAlpha = 0.15f,
    strokeAlpha = 0.15f,
  ) {
    moveTo(54f, 44f)
    verticalLineTo(22f)
    curveToRelative(-2.761f, 0f, -5f, 2.238f, -5f, 5f)
    verticalLineToRelative(17f)
    curveToRelative(0f, 2.757f, -2.243f, 5f, -5f, 5f)
    horizontalLineTo(33f)
    curveToRelative(-2.761f, 0f, -5f, 2.238f, -5f, 5f)
    horizontalLineToRelative(16f)
    curveTo(49.523f, 54f, 54f, 49.523f, 54f, 44f)
    close()
  }
  Path(
    stroke = SolidColor(Color.White),
    strokeLineWidth = 3f,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
  ) {
    moveTo(14f, 20f)
    curveToRelative(0f, -3.309f, 2.691f, -6f, 6f, -6f)
  }
}

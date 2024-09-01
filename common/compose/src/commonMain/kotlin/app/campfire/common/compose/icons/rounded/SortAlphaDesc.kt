package app.campfire.common.compose.icons.rounded

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var _SortAlphaAsc: ImageVector? = null

val Icons.Rounded.SortAlphaAsc: ImageVector
  get() {
    if (_SortAlphaAsc != null) {
      return _SortAlphaAsc!!
    }
    _SortAlphaAsc = ImageVector.Builder(
      name = "SortAlphaAsc",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f,
    ).apply {
      path(
        fill = SolidColor(Color.Black),
        fillAlpha = 1.0f,
        stroke = null,
        strokeAlpha = 1.0f,
        strokeLineWidth = 1.0f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Miter,
        strokeLineMiter = 1.0f,
        pathFillType = PathFillType.NonZero,
      ) {
        moveTo(7f, 19f)
        horizontalLineToRelative(3.013f)
        curveTo(10.558f, 19f, 11f, 19.442f, 11f, 19.987f)
        verticalLineToRelative(0.027f)
        curveTo(11f, 20.558f, 10.558f, 21f, 10.013f, 21f)
        horizontalLineTo(4.987f)
        curveTo(4.442f, 21f, 4f, 20.558f, 4f, 20.013f)
        verticalLineToRelative(-0.079f)
        curveToRelative(0f, -0.239f, 0.087f, -0.47f, 0.245f, -0.65f)
        lineTo(8f, 15f)
        horizontalLineTo(4.987f)
        curveTo(4.442f, 15f, 4f, 14.558f, 4f, 14.013f)
        verticalLineToRelative(-0.027f)
        curveTo(4f, 13.442f, 4.442f, 13f, 4.987f, 13f)
        horizontalLineToRelative(5.027f)
        curveTo(10.558f, 13f, 11f, 13.442f, 11f, 13.987f)
        verticalLineToRelative(0.064f)
        curveToRelative(0f, 0.239f, -0.087f, 0.469f, -0.244f, 0.649f)
        lineTo(7f, 19f)
        close()
      }
      path(
        fill = SolidColor(Color.Black),
        fillAlpha = 1.0f,
        stroke = null,
        strokeAlpha = 1.0f,
        strokeLineWidth = 1.0f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Miter,
        strokeLineMiter = 1.0f,
        pathFillType = PathFillType.NonZero,
      ) {
        moveTo(17.566f, 21.765f)
        lineToRelative(2.4f, -2.4f)
        curveToRelative(0.229f, -0.229f, 0.297f, -0.572f, 0.174f, -0.872f)
        curveTo(20.015f, 18.194f, 19.724f, 18f, 19.4f, 18f)
        horizontalLineToRelative(-4.8f)
        curveToRelative(-0.323f, 0f, -0.615f, 0.194f, -0.739f, 0.494f)
        curveTo(13.82f, 18.593f, 13.8f, 18.697f, 13.8f, 18.8f)
        curveToRelative(0f, 0.208f, 0.082f, 0.413f, 0.234f, 0.566f)
        lineToRelative(2.4f, 2.4f)
        curveTo(16.747f, 22.078f, 17.254f, 22.078f, 17.566f, 21.765f)
        close()
      }
      path(
        fill = null,
        fillAlpha = 1.0f,
        stroke = SolidColor(Color(0xFF000000)),
        strokeAlpha = 1.0f,
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Miter,
        strokeLineMiter = 10f,
        pathFillType = PathFillType.NonZero,
      ) {
        moveTo(17f, 20f)
        lineTo(17f, 3f)
      }
      path(
        fill = null,
        fillAlpha = 1.0f,
        stroke = SolidColor(Color(0xFF000000)),
        strokeAlpha = 1.0f,
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        strokeLineMiter = 10f,
        pathFillType = PathFillType.NonZero,
      ) {
        moveTo(5f, 9f)
        lineTo(7.5f, 3f)
        lineTo(10f, 9f)
      }
      path(
        fill = null,
        fillAlpha = 1.0f,
        stroke = SolidColor(Color(0xFF000000)),
        strokeAlpha = 1.0f,
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        strokeLineMiter = 10f,
        pathFillType = PathFillType.NonZero,
      ) {
        moveTo(6f, 8f)
        lineTo(9f, 8f)
      }
    }.build()
    return _SortAlphaAsc!!
  }


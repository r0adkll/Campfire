package app.campfire.common.compose.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var _TentRed: ImageVector? = null

val CampfireIcons.Tents.Red: ImageVector
  get() {
    if (_TentRed != null) {
      return _TentRed!!
    }
    _TentRed = ImageVector.Builder(
      name = "TentRed",
      defaultWidth = 64.dp,
      defaultHeight = 64.dp,
      viewportWidth = 64f,
      viewportHeight = 64f,
    ).apply {
      path(
        fill = SolidColor(Color(0xFFFD3C4F)),
        fillAlpha = 1.0f,
        stroke = null,
        strokeAlpha = 1.0f,
        strokeLineWidth = 1.0f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Miter,
        strokeLineMiter = 1.0f,
        pathFillType = PathFillType.NonZero,
      ) {
        moveTo(57.277f, 48f)
        horizontalLineTo(6.723f)
        curveToRelative(-2.692f, 0f, -4.492f, -2.77f, -3.399f, -5.23f)
        lineToRelative(11.828f, -26.613f)
        curveTo(16.276f, 13.629f, 18.783f, 12f, 21.549f, 12f)
        horizontalLineToRelative(20.902f)
        curveToRelative(2.766f, 0f, 5.273f, 1.629f, 6.397f, 4.157f)
        lineTo(60.676f, 42.77f)
        curveTo(61.769f, 45.23f, 59.968f, 48f, 57.277f, 48f)
        close()
      }
      path(
        fill = SolidColor(Color.Black),
        fillAlpha = .3f,
        stroke = null,
        strokeAlpha = .3f,
        strokeLineWidth = 1.0f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Miter,
        strokeLineMiter = 1.0f,
        pathFillType = PathFillType.NonZero,
      ) {
        moveTo(51f, 58f)
        arcTo(19f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 32f, 61f)
        arcTo(19f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 13f, 58f)
        arcTo(19f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 51f, 58f)
        close()
      }
      path(
        fill = SolidColor(Color(0xFFA61828)),
        fillAlpha = 1.0f,
        stroke = null,
        strokeAlpha = 1.0f,
        strokeLineWidth = 1.0f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Miter,
        strokeLineMiter = 1.0f,
        pathFillType = PathFillType.NonZero,
      ) {
        moveTo(6.723f, 48f)
        horizontalLineToRelative(30.831f)
        lineTo(24.463f, 18.599f)
        curveTo(24.03f, 17.627f, 23.065f, 17f, 22f, 17f)
        reflectiveCurveToRelative(-2.03f, 0.627f, -2.463f, 1.599f)
        lineTo(6.453f, 47.983f)
        curveTo(6.543f, 47.989f, 6.631f, 48f, 6.723f, 48f)
        close()
      }
      path(
        fill = SolidColor(Color.Black),
        fillAlpha = .15f,
        stroke = null,
        strokeAlpha = .15f,
        strokeLineWidth = 1.0f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Miter,
        strokeLineMiter = 1.0f,
        pathFillType = PathFillType.NonZero,
      ) {
        moveTo(52.935f, 25.354f)
        curveToRelative(-0.147f, 0.065f, -0.292f, 0.134f, -0.436f, 0.215f)
        curveToRelative(-2.278f, 1.278f, -3.081f, 4.183f, -2.02f, 6.57f)
        lineToRelative(4.203f, 9.455f)
        curveTo(54.976f, 42.255f, 54.492f, 43f, 53.768f, 43f)
        horizontalLineTo(43f)
        curveToRelative(-2.761f, 0f, -4.997f, 2.239f, -4.997f, 5f)
        horizontalLineToRelative(19.274f)
        curveToRelative(2.692f, 0f, 4.492f, -2.77f, 3.399f, -5.23f)
        lineTo(52.935f, 25.354f)
        close()
      }
      path(
        fill = SolidColor(Color(0xFFFFFFFF)),
        fillAlpha = .3f,
        stroke = null,
        strokeAlpha = .3f,
        strokeLineWidth = 1.0f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Miter,
        strokeLineMiter = 1.0f,
        pathFillType = PathFillType.NonZero,
      ) {
        moveTo(19.569f, 18.532f)
        curveTo(19.983f, 17.6f, 20.906f, 17f, 21.926f, 17f)
        horizontalLineTo(33f)
        curveToRelative(2.761f, 0f, 4.997f, -2.239f, 4.997f, -5f)
        horizontalLineTo(21.549f)
        curveToRelative(-2.766f, 0f, -5.273f, 1.629f, -6.397f, 4.157f)
        lineTo(3.324f, 42.77f)
        curveToRelative(-1.053f, 2.369f, 0.582f, 5.019f, 3.109f, 5.211f)
        lineTo(19.569f, 18.532f)
        close()
      }
      path(
        fill = null,
        fillAlpha = 1.0f,
        stroke = SolidColor(Color(0xFFFFFFFF)),
        strokeAlpha = 1.0f,
        strokeLineWidth = 3f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Miter,
        strokeLineMiter = 10f,
        pathFillType = PathFillType.NonZero,
      ) {
        moveTo(18.085f, 18.141f)
        curveToRelative(0.802f, -1.806f, 2.543f, -2.666f, 4.519f, -2.666f)
      }
    }.build()
    return _TentRed!!
  }

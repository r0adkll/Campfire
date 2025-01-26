package app.campfire.common.compose.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var _TentYellow: ImageVector? = null

val CampfireIcons.Tents.Yellow: ImageVector
  get() {
    if (_TentYellow != null) {
      return _TentYellow!!
    }
    _TentYellow = ImageVector.Builder(
      name = "TentYellow",
      defaultWidth = 64.dp,
      defaultHeight = 64.dp,
      viewportWidth = 64f,
      viewportHeight = 64f,
    ).apply {
      path(
        fill = SolidColor(Color(0xFFFDEF3C)),
        fillAlpha = 1.0f,
        stroke = null,
        strokeAlpha = 1.0f,
        strokeLineWidth = 1f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Miter,
        strokeLineMiter = 10f,
        pathFillType = PathFillType.NonZero,
      ) {
        moveTo(57.277f, 48f)
        horizontalLineToRelative(-50.554f)
        curveToRelative(-2.692f, 0f, -4.492f, -2.77f, -3.399f, -5.23f)
        lineToRelative(11.828f, -26.613f)
        curveToRelative(1.124f, -2.528f, 3.631f, -4.157f, 6.397f, -4.157f)
        horizontalLineToRelative(20.902f)
        curveToRelative(2.766f, 0f, 5.273f, 1.629f, 6.397f, 4.157f)
        lineToRelative(11.828f, 26.613f)
        curveToRelative(1.093f, 2.46f, -0.708f, 5.23f, -3.399f, 5.23f)
        close()
      }
      path(
        fill = SolidColor(Color(0xFF000000)),
        fillAlpha = 0.3f,
        stroke = null,
        strokeAlpha = 0.3f,
        strokeLineWidth = 1f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Miter,
        strokeLineMiter = 10f,
        pathFillType = PathFillType.NonZero,
      ) {
        moveTo(51f, 58f)
        arcTo(19f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 32f, 61f)
        arcTo(19f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 13f, 58f)
        arcTo(19f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 51f, 58f)
        close()
      }
      path(
        fill = SolidColor(Color(0xFFA69B18)),
        fillAlpha = 1.0f,
        stroke = null,
        strokeAlpha = 1.0f,
        strokeLineWidth = 1f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Miter,
        strokeLineMiter = 10f,
        pathFillType = PathFillType.NonZero,
      ) {
        moveTo(6.723f, 48f)
        horizontalLineToRelative(30.831f)
        lineToRelative(-13.091f, -29.401f)
        curveToRelative(-0.433f, -0.972f, -1.398f, -1.599f, -2.463f, -1.599f)
        curveToRelative(-1.065f, 0f, -2.03f, 0.627f, -2.463f, 1.599f)
        lineToRelative(-13.084f, 29.384f)
        curveToRelative(0.09f, 0.006f, 0.178f, 0.017f, 0.27f, 0.017f)
        close()
      }
      path(
        fill = SolidColor(Color(0xFF000000)),
        fillAlpha = 0.15f,
        stroke = null,
        strokeAlpha = 0.15f,
        strokeLineWidth = 1f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Miter,
        strokeLineMiter = 10f,
        pathFillType = PathFillType.NonZero,
      ) {
        moveTo(52.935f, 25.354f)
        curveToRelative(-0.147f, 0.065f, -0.292f, 0.134f, -0.436f, 0.215f)
        curveToRelative(-2.278f, 1.278f, -3.081f, 4.183f, -2.02f, 6.57f)
        lineToRelative(4.203f, 9.455f)
        curveToRelative(0.294f, 0.661f, -0.19f, 1.406f, -0.914f, 1.406f)
        horizontalLineToRelative(-10.768f)
        curveToRelative(-2.761f, 0f, -4.997f, 2.239f, -4.997f, 5f)
        horizontalLineToRelative(19.274f)
        curveToRelative(2.692f, 0f, 4.492f, -2.77f, 3.399f, -5.23f)
        close()
      }
      path(
        fill = SolidColor(Color(0xFFFFFFFF)),
        fillAlpha = 0.3f,
        stroke = null,
        strokeAlpha = 0.3f,
        strokeLineWidth = 1f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Miter,
        strokeLineMiter = 10f,
        pathFillType = PathFillType.NonZero,
      ) {
        moveTo(19.569f, 18.532f)
        curveToRelative(0.414f, -0.932f, 1.337f, -1.532f, 2.357f, -1.532f)
        horizontalLineToRelative(11.074f)
        curveToRelative(2.761f, 0f, 4.997f, -2.239f, 4.997f, -5f)
        horizontalLineToRelative(-16.448f)
        curveToRelative(-2.766f, 0f, -5.273f, 1.629f, -6.397f, 4.157f)
        lineToRelative(-11.828f, 26.613f)
        curveToRelative(-1.053f, 2.369f, 0.582f, 5.019f, 3.109f, 5.211f)
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
    return _TentYellow!!
  }

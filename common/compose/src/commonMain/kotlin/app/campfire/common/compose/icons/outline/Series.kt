package app.campfire.common.compose.icons.outline

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var _SeriesOutline: ImageVector? = null

val Icons.Outlined.Series: ImageVector
  get() {
    if (_SeriesOutline != null) {
      return _SeriesOutline!!
    }
    _SeriesOutline = ImageVector.Builder(
      name = "SeriesOutline",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f,
    ).apply {
      path(
        fill = SolidColor(Color(0xFF000000)),
        fillAlpha = 1.0f,
        stroke = null,
        strokeAlpha = 1.0f,
        strokeLineWidth = 1.0f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Miter,
        strokeLineMiter = 1.0f,
        pathFillType = PathFillType.NonZero,
      ) {
        moveTo(3f, 17f)
        verticalLineTo(7f)
        curveTo(3f, 6.45f, 3.1957f, 5.9793f, 3.587f, 5.588f)
        curveTo(3.979f, 5.196f, 4.45f, 5f, 5f, 5f)
        horizontalLineTo(18.975f)
        curveTo(19.525f, 5f, 19.996f, 5.196f, 20.388f, 5.588f)
        curveTo(20.7793f, 5.9793f, 20.975f, 6.45f, 20.975f, 7f)
        verticalLineTo(17f)
        curveTo(20.975f, 17.55f, 20.7793f, 18.021f, 20.388f, 18.413f)
        curveTo(19.996f, 18.8043f, 19.525f, 19f, 18.975f, 19f)
        horizontalLineTo(5f)
        curveTo(4.45f, 19f, 3.979f, 18.8043f, 3.587f, 18.413f)
        curveTo(3.1957f, 18.021f, 3f, 17.55f, 3f, 17f)
        close()
        moveTo(5f, 17f)
        horizontalLineTo(8.325f)
        verticalLineTo(7f)
        horizontalLineTo(5f)
        verticalLineTo(17f)
        close()
        moveTo(10.325f, 17f)
        horizontalLineTo(13.65f)
        verticalLineTo(7f)
        horizontalLineTo(10.325f)
        verticalLineTo(17f)
        close()
        moveTo(15.65f, 17f)
        horizontalLineTo(18.975f)
        verticalLineTo(7f)
        horizontalLineTo(15.65f)
        verticalLineTo(17f)
        close()
      }
    }.build()
    return _SeriesOutline!!
  }

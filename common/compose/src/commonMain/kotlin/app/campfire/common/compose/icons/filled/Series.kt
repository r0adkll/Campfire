package app.campfire.common.compose.icons.filled

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var _SeriesFilled: ImageVector? = null

val Icons.Filled.Series: ImageVector
  get() {
    if (_SeriesFilled != null) {
      return _SeriesFilled!!
    }
    _SeriesFilled = ImageVector.Builder(
      name = "SeriesFilled",
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
        moveTo(14.67f, 6f)
        verticalLineTo(18f)
        curveTo(14.67f, 18.55f, 14.22f, 19f, 13.67f, 19f)
        horizontalLineTo(10.34f)
        curveTo(9.79f, 19f, 9.34f, 18.55f, 9.34f, 18f)
        verticalLineTo(6f)
        curveTo(9.34f, 5.45f, 9.79f, 5f, 10.34f, 5f)
        horizontalLineTo(13.67f)
        curveTo(14.22f, 5f, 14.67f, 5.45f, 14.67f, 6f)
        close()
        moveTo(16.67f, 19f)
        horizontalLineTo(20f)
        curveTo(20.55f, 19f, 21f, 18.55f, 21f, 18f)
        verticalLineTo(6f)
        curveTo(21f, 5.45f, 20.55f, 5f, 20f, 5f)
        horizontalLineTo(16.67f)
        curveTo(16.12f, 5f, 15.67f, 5.45f, 15.67f, 6f)
        verticalLineTo(18f)
        curveTo(15.67f, 18.55f, 16.11f, 19f, 16.67f, 19f)
        close()
        moveTo(8.33f, 18f)
        verticalLineTo(6f)
        curveTo(8.33f, 5.45f, 7.88f, 5f, 7.33f, 5f)
        horizontalLineTo(4f)
        curveTo(3.45f, 5f, 3f, 5.45f, 3f, 6f)
        verticalLineTo(18f)
        curveTo(3f, 18.55f, 3.45f, 19f, 4f, 19f)
        horizontalLineTo(7.33f)
        curveTo(7.89f, 19f, 8.33f, 18.55f, 8.33f, 18f)
        close()
      }
    }.build()
    return _SeriesFilled!!
  }

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

private var _HomeFilled: ImageVector? = null

val Icons.Filled.Home: ImageVector
  get() {
    if (_HomeFilled != null) {
      return _HomeFilled!!
    }
    _HomeFilled = ImageVector.Builder(
      name = "HomeFilled",
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
        moveTo(6f, 21f)
        curveTo(5.45f, 21f, 4.9793f, 20.8043f, 4.588f, 20.413f)
        curveTo(4.196f, 20.021f, 4f, 19.55f, 4f, 19f)
        verticalLineTo(10f)
        curveTo(4f, 9.6833f, 4.071f, 9.3833f, 4.213f, 9.1f)
        curveTo(4.3543f, 8.8167f, 4.55f, 8.5833f, 4.8f, 8.4f)
        lineTo(10.8f, 3.9f)
        curveTo(10.9833f, 3.7667f, 11.175f, 3.6667f, 11.375f, 3.6f)
        curveTo(11.575f, 3.5333f, 11.7833f, 3.5f, 12f, 3.5f)
        curveTo(12.2167f, 3.5f, 12.425f, 3.5333f, 12.625f, 3.6f)
        curveTo(12.825f, 3.6667f, 13.0167f, 3.7667f, 13.2f, 3.9f)
        lineTo(19.2f, 8.4f)
        curveTo(19.45f, 8.5833f, 19.646f, 8.8167f, 19.788f, 9.1f)
        curveTo(19.9293f, 9.3833f, 20f, 9.6833f, 20f, 10f)
        verticalLineTo(19f)
        curveTo(20f, 19.55f, 19.8043f, 20.021f, 19.413f, 20.413f)
        curveTo(19.021f, 20.8043f, 18.55f, 21f, 18f, 21f)
        horizontalLineTo(14f)
        verticalLineTo(14f)
        horizontalLineTo(10f)
        verticalLineTo(21f)
        horizontalLineTo(6f)
        close()
      }
    }.build()
    return _HomeFilled!!
  }

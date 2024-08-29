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

private var _AuthorFilled: ImageVector? = null

val Icons.Filled.Author: ImageVector
  get() {
    if (_AuthorFilled != null) {
      return _AuthorFilled!!
    }
    _AuthorFilled = ImageVector.Builder(
      name = "AuthorFilled",
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
        moveTo(12f, 12f)
        curveTo(14.21f, 12f, 16f, 10.21f, 16f, 8f)
        curveTo(16f, 5.79f, 14.21f, 4f, 12f, 4f)
        curveTo(9.79f, 4f, 8f, 5.79f, 8f, 8f)
        curveTo(8f, 10.21f, 9.79f, 12f, 12f, 12f)
        close()
        moveTo(12f, 14f)
        curveTo(9.33f, 14f, 4f, 15.34f, 4f, 18f)
        verticalLineTo(19f)
        curveTo(4f, 19.55f, 4.45f, 20f, 5f, 20f)
        horizontalLineTo(19f)
        curveTo(19.55f, 20f, 20f, 19.55f, 20f, 19f)
        verticalLineTo(18f)
        curveTo(20f, 15.34f, 14.67f, 14f, 12f, 14f)
        close()
      }
    }.build()
    return _AuthorFilled!!
  }

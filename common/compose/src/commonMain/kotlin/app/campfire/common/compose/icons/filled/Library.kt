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

private var _LibraryFilled: ImageVector? = null

val Icons.Filled.Library: ImageVector
  get() {
    if (_LibraryFilled != null) {
      return _LibraryFilled!!
    }
    _LibraryFilled = ImageVector.Builder(
      name = "LibraryFilled",
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
        moveTo(12f, 11.55f)
        curveTo(10.18f, 9.85f, 7.88f, 8.66f, 5.32f, 8.2f)
        curveTo(4.11f, 7.99f, 3f, 8.95f, 3f, 10.18f)
        verticalLineTo(16.42f)
        curveTo(3f, 18.1f, 3.72f, 18.98f, 4.71f, 19.11f)
        curveTo(7.21f, 19.43f, 9.48f, 20.46f, 11.34f, 21.98f)
        curveTo(11.69f, 22.27f, 12.26f, 22.3f, 12.61f, 22.02f)
        curveTo(14.48f, 20.49f, 16.77f, 19.44f, 19.29f, 19.12f)
        curveTo(20.23f, 18.99f, 21f, 18.06f, 21f, 17.1f)
        verticalLineTo(10.18f)
        curveTo(21f, 8.95f, 19.89f, 7.99f, 18.68f, 8.2f)
        curveTo(16.12f, 8.66f, 13.82f, 9.85f, 12f, 11.55f)
        close()
        moveTo(12f, 8f)
        curveTo(13.66f, 8f, 15f, 6.66f, 15f, 5f)
        curveTo(15f, 3.34f, 13.66f, 2f, 12f, 2f)
        curveTo(10.34f, 2f, 9f, 3.34f, 9f, 5f)
        curveTo(9f, 6.66f, 10.34f, 8f, 12f, 8f)
        close()
      }
    }.build()
    return _LibraryFilled!!
  }

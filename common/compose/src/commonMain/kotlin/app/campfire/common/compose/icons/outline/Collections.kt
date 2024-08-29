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

private var _CollectionsOutline: ImageVector? = null

val Icons.Outlined.Collections: ImageVector
  get() {
    if (_CollectionsOutline != null) {
      return _CollectionsOutline!!
    }
    _CollectionsOutline = ImageVector.Builder(
      name = "CollectionsOutline",
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
        moveTo(8f, 16f)
        horizontalLineTo(20f)
        verticalLineTo(4f)
        horizontalLineTo(18f)
        verticalLineTo(10.125f)
        curveTo(18f, 10.325f, 17.9167f, 10.4707f, 17.75f, 10.562f)
        curveTo(17.5833f, 10.654f, 17.4167f, 10.65f, 17.25f, 10.55f)
        lineTo(15.5f, 9.5f)
        lineTo(13.75f, 10.55f)
        curveTo(13.5833f, 10.65f, 13.4167f, 10.654f, 13.25f, 10.562f)
        curveTo(13.0833f, 10.4707f, 13f, 10.325f, 13f, 10.125f)
        verticalLineTo(4f)
        horizontalLineTo(8f)
        verticalLineTo(16f)
        close()
        moveTo(8f, 18f)
        curveTo(7.45f, 18f, 6.9793f, 17.8043f, 6.588f, 17.413f)
        curveTo(6.196f, 17.021f, 6f, 16.55f, 6f, 16f)
        verticalLineTo(4f)
        curveTo(6f, 3.45f, 6.196f, 2.979f, 6.588f, 2.587f)
        curveTo(6.9793f, 2.1957f, 7.45f, 2f, 8f, 2f)
        horizontalLineTo(20f)
        curveTo(20.55f, 2f, 21.021f, 2.1957f, 21.413f, 2.587f)
        curveTo(21.8043f, 2.979f, 22f, 3.45f, 22f, 4f)
        verticalLineTo(16f)
        curveTo(22f, 16.55f, 21.8043f, 17.021f, 21.413f, 17.413f)
        curveTo(21.021f, 17.8043f, 20.55f, 18f, 20f, 18f)
        horizontalLineTo(8f)
        close()
        moveTo(4f, 22f)
        curveTo(3.45f, 22f, 2.9793f, 21.8043f, 2.588f, 21.413f)
        curveTo(2.196f, 21.021f, 2f, 20.55f, 2f, 20f)
        verticalLineTo(7f)
        curveTo(2f, 6.7167f, 2.096f, 6.479f, 2.288f, 6.287f)
        curveTo(2.4793f, 6.0957f, 2.7167f, 6f, 3f, 6f)
        curveTo(3.2833f, 6f, 3.521f, 6.0957f, 3.713f, 6.287f)
        curveTo(3.9043f, 6.479f, 4f, 6.7167f, 4f, 7f)
        verticalLineTo(20f)
        horizontalLineTo(17f)
        curveTo(17.2833f, 20f, 17.5207f, 20.096f, 17.712f, 20.288f)
        curveTo(17.904f, 20.4793f, 18f, 20.7167f, 18f, 21f)
        curveTo(18f, 21.2833f, 17.904f, 21.5207f, 17.712f, 21.712f)
        curveTo(17.5207f, 21.904f, 17.2833f, 22f, 17f, 22f)
        horizontalLineTo(4f)
        close()
      }
    }.build()
    return _CollectionsOutline!!
  }

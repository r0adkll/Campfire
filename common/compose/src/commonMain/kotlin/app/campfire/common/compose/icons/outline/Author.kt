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

private var _AuthorOutline: ImageVector? = null

val Icons.Outlined.Author: ImageVector
  get() {
    if (_AuthorOutline != null) {
      return _AuthorOutline!!
    }
    _AuthorOutline = ImageVector.Builder(
      name = "AuthorOutline",
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
        curveTo(10.9f, 12f, 9.9583f, 11.6083f, 9.175f, 10.825f)
        curveTo(8.3917f, 10.0417f, 8f, 9.1f, 8f, 8f)
        curveTo(8f, 6.9f, 8.3917f, 5.9583f, 9.175f, 5.175f)
        curveTo(9.9583f, 4.3917f, 10.9f, 4f, 12f, 4f)
        curveTo(13.1f, 4f, 14.0417f, 4.3917f, 14.825f, 5.175f)
        curveTo(15.6083f, 5.9583f, 16f, 6.9f, 16f, 8f)
        curveTo(16f, 9.1f, 15.6083f, 10.0417f, 14.825f, 10.825f)
        curveTo(14.0417f, 11.6083f, 13.1f, 12f, 12f, 12f)
        close()
        moveTo(18f, 20f)
        horizontalLineTo(6f)
        curveTo(5.45f, 20f, 4.9793f, 19.8043f, 4.588f, 19.413f)
        curveTo(4.196f, 19.021f, 4f, 18.55f, 4f, 18f)
        verticalLineTo(17.2f)
        curveTo(4f, 16.6333f, 4.146f, 16.1123f, 4.438f, 15.637f)
        curveTo(4.7293f, 15.1623f, 5.1167f, 14.8f, 5.6f, 14.55f)
        curveTo(6.6333f, 14.0333f, 7.6833f, 13.6457f, 8.75f, 13.387f)
        curveTo(9.8167f, 13.129f, 10.9f, 13f, 12f, 13f)
        curveTo(13.1f, 13f, 14.1833f, 13.129f, 15.25f, 13.387f)
        curveTo(16.3167f, 13.6457f, 17.3667f, 14.0333f, 18.4f, 14.55f)
        curveTo(18.8833f, 14.8f, 19.2707f, 15.1623f, 19.562f, 15.637f)
        curveTo(19.854f, 16.1123f, 20f, 16.6333f, 20f, 17.2f)
        verticalLineTo(18f)
        curveTo(20f, 18.55f, 19.8043f, 19.021f, 19.413f, 19.413f)
        curveTo(19.021f, 19.8043f, 18.55f, 20f, 18f, 20f)
        close()
        moveTo(6f, 18f)
        horizontalLineTo(18f)
        verticalLineTo(17.2f)
        curveTo(18f, 17.0167f, 17.9543f, 16.85f, 17.863f, 16.7f)
        curveTo(17.771f, 16.55f, 17.65f, 16.4333f, 17.5f, 16.35f)
        curveTo(16.6f, 15.9f, 15.6917f, 15.5623f, 14.775f, 15.337f)
        curveTo(13.8583f, 15.1123f, 12.9333f, 15f, 12f, 15f)
        curveTo(11.0667f, 15f, 10.1417f, 15.1123f, 9.225f, 15.337f)
        curveTo(8.3083f, 15.5623f, 7.4f, 15.9f, 6.5f, 16.35f)
        curveTo(6.35f, 16.4333f, 6.2293f, 16.55f, 6.138f, 16.7f)
        curveTo(6.046f, 16.85f, 6f, 17.0167f, 6f, 17.2f)
        verticalLineTo(18f)
        close()
        moveTo(12f, 10f)
        curveTo(12.55f, 10f, 13.021f, 9.804f, 13.413f, 9.412f)
        curveTo(13.8043f, 9.0207f, 14f, 8.55f, 14f, 8f)
        curveTo(14f, 7.45f, 13.8043f, 6.9793f, 13.413f, 6.588f)
        curveTo(13.021f, 6.196f, 12.55f, 6f, 12f, 6f)
        curveTo(11.45f, 6f, 10.9793f, 6.196f, 10.588f, 6.588f)
        curveTo(10.196f, 6.9793f, 10f, 7.45f, 10f, 8f)
        curveTo(10f, 8.55f, 10.196f, 9.0207f, 10.588f, 9.412f)
        curveTo(10.9793f, 9.804f, 11.45f, 10f, 12f, 10f)
        close()
      }
    }.build()
    return _AuthorOutline!!
  }

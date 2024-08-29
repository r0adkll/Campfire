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

private var _LibraryOutline: ImageVector? = null

val Icons.Outlined.Library: ImageVector
  get() {
    if (_LibraryOutline != null) {
      return _LibraryOutline!!
    }
    _LibraryOutline = ImageVector.Builder(
      name = "LibraryOutline",
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
        moveTo(10.85f, 21.525f)
        curveTo(9.95f, 20.875f, 8.9877f, 20.35f, 7.963f, 19.95f)
        curveTo(6.9377f, 19.55f, 5.8667f, 19.275f, 4.75f, 19.125f)
        curveTo(4.25f, 19.0583f, 3.8333f, 18.8457f, 3.5f, 18.487f)
        curveTo(3.1667f, 18.129f, 3f, 17.7167f, 3f, 17.25f)
        verticalLineTo(10.2f)
        curveTo(3f, 9.5667f, 3.2167f, 9.0457f, 3.65f, 8.637f)
        curveTo(4.0833f, 8.229f, 4.5917f, 8.075f, 5.175f, 8.175f)
        curveTo(6.4583f, 8.3917f, 7.6793f, 8.7833f, 8.838f, 9.35f)
        curveTo(9.996f, 9.9167f, 11.05f, 10.65f, 12f, 11.55f)
        curveTo(12.95f, 10.65f, 14.0043f, 9.9167f, 15.163f, 9.35f)
        curveTo(16.321f, 8.7833f, 17.5417f, 8.3917f, 18.825f, 8.175f)
        curveTo(19.4083f, 8.075f, 19.9167f, 8.229f, 20.35f, 8.637f)
        curveTo(20.7833f, 9.0457f, 21f, 9.5667f, 21f, 10.2f)
        verticalLineTo(17.25f)
        curveTo(21f, 17.7167f, 20.8333f, 18.129f, 20.5f, 18.487f)
        curveTo(20.1667f, 18.8457f, 19.75f, 19.0583f, 19.25f, 19.125f)
        curveTo(18.1333f, 19.275f, 17.0627f, 19.55f, 16.038f, 19.95f)
        curveTo(15.0127f, 20.35f, 14.05f, 20.875f, 13.15f, 21.525f)
        curveTo(12.8167f, 21.775f, 12.4333f, 21.9f, 12f, 21.9f)
        curveTo(11.5667f, 21.9f, 11.1833f, 21.775f, 10.85f, 21.525f)
        close()
        moveTo(12f, 19.9f)
        curveTo(13.05f, 19.1167f, 14.1667f, 18.4917f, 15.35f, 18.025f)
        curveTo(16.5333f, 17.5583f, 17.75f, 17.25f, 19f, 17.1f)
        verticalLineTo(10.2f)
        curveTo(17.7833f, 10.4167f, 16.5877f, 10.854f, 15.413f, 11.512f)
        curveTo(14.2377f, 12.1707f, 13.1f, 13.05f, 12f, 14.15f)
        curveTo(10.9f, 13.05f, 9.7627f, 12.1707f, 8.588f, 11.512f)
        curveTo(7.4127f, 10.854f, 6.2167f, 10.4167f, 5f, 10.2f)
        verticalLineTo(17.1f)
        curveTo(6.25f, 17.25f, 7.4667f, 17.5583f, 8.65f, 18.025f)
        curveTo(9.8333f, 18.4917f, 10.95f, 19.1167f, 12f, 19.9f)
        close()
        moveTo(12f, 9f)
        curveTo(10.9f, 9f, 9.9583f, 8.6083f, 9.175f, 7.825f)
        curveTo(8.3917f, 7.0417f, 8f, 6.1f, 8f, 5f)
        curveTo(8f, 3.9f, 8.3917f, 2.9583f, 9.175f, 2.175f)
        curveTo(9.9583f, 1.3917f, 10.9f, 1f, 12f, 1f)
        curveTo(13.1f, 1f, 14.0417f, 1.3917f, 14.825f, 2.175f)
        curveTo(15.6083f, 2.9583f, 16f, 3.9f, 16f, 5f)
        curveTo(16f, 6.1f, 15.6083f, 7.0417f, 14.825f, 7.825f)
        curveTo(14.0417f, 8.6083f, 13.1f, 9f, 12f, 9f)
        close()
        moveTo(12f, 7f)
        curveTo(12.55f, 7f, 13.021f, 6.804f, 13.413f, 6.412f)
        curveTo(13.8043f, 6.0207f, 14f, 5.55f, 14f, 5f)
        curveTo(14f, 4.45f, 13.8043f, 3.979f, 13.413f, 3.587f)
        curveTo(13.021f, 3.1957f, 12.55f, 3f, 12f, 3f)
        curveTo(11.45f, 3f, 10.9793f, 3.1957f, 10.588f, 3.587f)
        curveTo(10.196f, 3.979f, 10f, 4.45f, 10f, 5f)
        curveTo(10f, 5.55f, 10.196f, 6.0207f, 10.588f, 6.412f)
        curveTo(10.9793f, 6.804f, 11.45f, 7f, 12f, 7f)
        close()
      }
    }.build()
    return _LibraryOutline!!
  }

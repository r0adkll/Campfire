package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

private var _Accountswitch: ImageVector? = null

val CampfireIcons.Rounded.AccountSwitch: ImageVector
  get() {
    if (_Accountswitch != null) {
      return _Accountswitch!!
    }
    _Accountswitch = ImageVector.Builder(
      name = "Accountswitch",
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
        moveTo(1f, 18f)
        verticalLineTo(6f)
        curveTo(1f, 5.45f, 1.1958f, 4.9792f, 1.5875f, 4.5875f)
        curveTo(1.9792f, 4.1958f, 2.45f, 4f, 3f, 4f)
        horizontalLineTo(9f)
        curveTo(9.55f, 4f, 10.0208f, 4.1958f, 10.4125f, 4.5875f)
        curveTo(10.8042f, 4.9792f, 11f, 5.45f, 11f, 6f)
        verticalLineTo(7f)
        curveTo(11f, 7.2833f, 10.9042f, 7.5208f, 10.7125f, 7.7125f)
        curveTo(10.5208f, 7.9042f, 10.2833f, 8f, 10f, 8f)
        curveTo(9.7167f, 8f, 9.4792f, 7.9042f, 9.2875f, 7.7125f)
        curveTo(9.0958f, 7.5208f, 9f, 7.2833f, 9f, 7f)
        verticalLineTo(6f)
        horizontalLineTo(3f)
        verticalLineTo(18f)
        horizontalLineTo(9f)
        verticalLineTo(17f)
        curveTo(9f, 16.7167f, 9.0958f, 16.4792f, 9.2875f, 16.2875f)
        curveTo(9.4792f, 16.0958f, 9.7167f, 16f, 10f, 16f)
        curveTo(10.2833f, 16f, 10.5208f, 16.0958f, 10.7125f, 16.2875f)
        curveTo(10.9042f, 16.4792f, 11f, 16.7167f, 11f, 17f)
        verticalLineTo(18f)
        curveTo(11f, 18.55f, 10.8042f, 19.0208f, 10.4125f, 19.4125f)
        curveTo(10.0208f, 19.8042f, 9.55f, 20f, 9f, 20f)
        horizontalLineTo(3f)
        curveTo(2.45f, 20f, 1.9792f, 19.8042f, 1.5875f, 19.4125f)
        curveTo(1.1958f, 19.0208f, 1f, 18.55f, 1f, 18f)
        close()
        moveTo(13f, 18f)
        verticalLineTo(17f)
        curveTo(13f, 16.7167f, 13.0958f, 16.4792f, 13.2875f, 16.2875f)
        curveTo(13.4792f, 16.0958f, 13.7167f, 16f, 14f, 16f)
        curveTo(14.2833f, 16f, 14.5208f, 16.0958f, 14.7125f, 16.2875f)
        curveTo(14.9042f, 16.4792f, 15f, 16.7167f, 15f, 17f)
        verticalLineTo(18f)
        horizontalLineTo(16f)
        curveTo(16.2833f, 18f, 16.5208f, 18.0958f, 16.7125f, 18.2875f)
        curveTo(16.9042f, 18.4792f, 17f, 18.7167f, 17f, 19f)
        curveTo(17f, 19.2833f, 16.9042f, 19.5208f, 16.7125f, 19.7125f)
        curveTo(16.5208f, 19.9042f, 16.2833f, 20f, 16f, 20f)
        horizontalLineTo(15f)
        curveTo(14.45f, 20f, 13.9792f, 19.8042f, 13.5875f, 19.4125f)
        curveTo(13.1958f, 19.0208f, 13f, 18.55f, 13f, 18f)
        close()
        moveTo(20f, 18f)
        horizontalLineTo(21f)
        verticalLineTo(17f)
        curveTo(21f, 16.7167f, 21.0958f, 16.4792f, 21.2875f, 16.2875f)
        curveTo(21.4792f, 16.0958f, 21.7167f, 16f, 22f, 16f)
        curveTo(22.2833f, 16f, 22.5208f, 16.0958f, 22.7125f, 16.2875f)
        curveTo(22.9042f, 16.4792f, 23f, 16.7167f, 23f, 17f)
        verticalLineTo(18f)
        curveTo(23f, 18.55f, 22.8042f, 19.0208f, 22.4125f, 19.4125f)
        curveTo(22.0208f, 19.8042f, 21.55f, 20f, 21f, 20f)
        horizontalLineTo(20f)
        curveTo(19.7167f, 20f, 19.4792f, 19.9042f, 19.2875f, 19.7125f)
        curveTo(19.0958f, 19.5208f, 19f, 19.2833f, 19f, 19f)
        curveTo(19f, 18.7167f, 19.0958f, 18.4792f, 19.2875f, 18.2875f)
        curveTo(19.4792f, 18.0958f, 19.7167f, 18f, 20f, 18f)
        close()
        moveTo(13f, 7f)
        verticalLineTo(6f)
        curveTo(13f, 5.45f, 13.1958f, 4.9792f, 13.5875f, 4.5875f)
        curveTo(13.9792f, 4.1958f, 14.45f, 4f, 15f, 4f)
        horizontalLineTo(16f)
        curveTo(16.2833f, 4f, 16.5208f, 4.0958f, 16.7125f, 4.2875f)
        curveTo(16.9042f, 4.4792f, 17f, 4.7167f, 17f, 5f)
        curveTo(17f, 5.2833f, 16.9042f, 5.5208f, 16.7125f, 5.7125f)
        curveTo(16.5208f, 5.9042f, 16.2833f, 6f, 16f, 6f)
        horizontalLineTo(15f)
        verticalLineTo(7f)
        curveTo(15f, 7.2833f, 14.9042f, 7.5208f, 14.7125f, 7.7125f)
        curveTo(14.5208f, 7.9042f, 14.2833f, 8f, 14f, 8f)
        curveTo(13.7167f, 8f, 13.4792f, 7.9042f, 13.2875f, 7.7125f)
        curveTo(13.0958f, 7.5208f, 13f, 7.2833f, 13f, 7f)
        close()
        moveTo(21f, 7f)
        verticalLineTo(6f)
        horizontalLineTo(20f)
        curveTo(19.7167f, 6f, 19.4792f, 5.9042f, 19.2875f, 5.7125f)
        curveTo(19.0958f, 5.5208f, 19f, 5.2833f, 19f, 5f)
        curveTo(19f, 4.7167f, 19.0958f, 4.4792f, 19.2875f, 4.2875f)
        curveTo(19.4792f, 4.0958f, 19.7167f, 4f, 20f, 4f)
        horizontalLineTo(21f)
        curveTo(21.55f, 4f, 22.0208f, 4.1958f, 22.4125f, 4.5875f)
        curveTo(22.8042f, 4.9792f, 23f, 5.45f, 23f, 6f)
        verticalLineTo(7f)
        curveTo(23f, 7.2833f, 22.9042f, 7.5208f, 22.7125f, 7.7125f)
        curveTo(22.5208f, 7.9042f, 22.2833f, 8f, 22f, 8f)
        curveTo(21.7167f, 8f, 21.4792f, 7.9042f, 21.2875f, 7.7125f)
        curveTo(21.0958f, 7.5208f, 21f, 7.2833f, 21f, 7f)
        close()
        moveTo(17.175f, 13f)
        horizontalLineTo(7f)
        curveTo(6.7167f, 13f, 6.4792f, 12.9042f, 6.2875f, 12.7125f)
        curveTo(6.0958f, 12.5208f, 6f, 12.2833f, 6f, 12f)
        curveTo(6f, 11.7167f, 6.0958f, 11.4792f, 6.2875f, 11.2875f)
        curveTo(6.4792f, 11.0958f, 6.7167f, 11f, 7f, 11f)
        horizontalLineTo(17.175f)
        lineTo(16.275f, 10.1f)
        curveTo(16.0917f, 9.9167f, 16f, 9.6875f, 16f, 9.4125f)
        curveTo(16f, 9.1375f, 16.1f, 8.9f, 16.3f, 8.7f)
        curveTo(16.4833f, 8.5167f, 16.7167f, 8.425f, 17f, 8.425f)
        curveTo(17.2833f, 8.425f, 17.5167f, 8.5167f, 17.7f, 8.7f)
        lineTo(20.3f, 11.3f)
        curveTo(20.5f, 11.5f, 20.6f, 11.7333f, 20.6f, 12f)
        curveTo(20.6f, 12.2667f, 20.5f, 12.5f, 20.3f, 12.7f)
        lineTo(17.7f, 15.3f)
        curveTo(17.5167f, 15.4833f, 17.2875f, 15.5792f, 17.0125f, 15.5875f)
        curveTo(16.7375f, 15.5958f, 16.5f, 15.5f, 16.3f, 15.3f)
        curveTo(16.1167f, 15.1167f, 16.025f, 14.8833f, 16.025f, 14.6f)
        curveTo(16.025f, 14.3167f, 16.1167f, 14.0833f, 16.3f, 13.9f)
        lineTo(17.175f, 13f)
        close()
      }
    }.build()
    return _Accountswitch!!
  }

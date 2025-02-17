package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.LogFile: ImageVector
  get() {
    if (_LogFile != null) {
      return _LogFile!!
    }
    _LogFile = ImageVector.Builder(
      name = "Rounded.LogFile",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f,
    ).apply {
      path(fill = SolidColor(Color(0xFF000000))) {
        moveTo(6f, 2f)
        curveTo(4.906f, 2f, 4f, 2.906f, 4f, 4f)
        lineTo(4f, 9f)
        curveTo(2.895f, 9f, 2f, 9.895f, 2f, 11f)
        lineTo(2f, 16f)
        curveTo(2f, 17.105f, 2.895f, 18f, 4f, 18f)
        lineTo(4f, 20f)
        curveTo(4f, 21.094f, 4.906f, 22f, 6f, 22f)
        lineTo(18f, 22f)
        curveTo(19.094f, 22f, 20f, 21.094f, 20f, 20f)
        lineTo(20f, 7.414f)
        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 19.707f, 6.707f)
        lineTo(15.293f, 2.293f)
        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 14.586f, 2f)
        lineTo(6f, 2f)
        close()
        moveTo(6f, 4f)
        lineTo(14f, 4f)
        lineTo(14f, 7f)
        curveTo(14f, 7.552f, 14.448f, 8f, 15f, 8f)
        lineTo(18f, 8f)
        lineTo(18f, 9f)
        lineTo(6f, 9f)
        lineTo(6f, 4f)
        close()
        moveTo(4f, 11f)
        lineTo(4.832f, 11f)
        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 5f, 11.014f)
        lineTo(5f, 15f)
        lineTo(7f, 15f)
        lineTo(7f, 16f)
        lineTo(5.168f, 16f)
        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 4.842f, 16f)
        lineTo(4f, 16f)
        lineTo(4f, 11f)
        close()
        moveTo(10f, 11.002f)
        curveTo(11.864f, 10.972f, 12f, 12.782f, 12f, 13.131f)
        lineTo(12f, 13.938f)
        curveTo(12f, 14.28f, 11.914f, 16.039f, 10.008f, 15.998f)
        curveTo(7.967f, 15.955f, 8f, 14.278f, 8f, 13.938f)
        lineTo(8f, 13.131f)
        curveTo(8f, 12.78f, 8.167f, 11.031f, 10f, 11.002f)
        close()
        moveTo(15.035f, 11.002f)
        curveTo(16.207f, 11.003f, 16.673f, 11.539f, 16.863f, 12.002f)
        lineTo(15f, 12.002f)
        curveTo(14.118f, 11.989f, 14f, 12.857f, 14f, 13.086f)
        lineTo(14f, 13.914f)
        curveTo(14f, 14.146f, 14.207f, 15.012f, 15.176f, 15.002f)
        curveTo(15.459f, 14.999f, 15.976f, 14.836f, 16f, 14.822f)
        lineTo(15.979f, 14f)
        lineTo(15f, 14f)
        lineTo(15.021f, 13f)
        lineTo(17f, 13f)
        lineTo(17f, 15.381f)
        curveTo(16.94f, 15.437f, 16.291f, 15.961f, 15.107f, 15.998f)
        curveTo(14.785f, 16.008f, 13.132f, 16.038f, 13f, 13.914f)
        lineTo(13f, 13.092f)
        curveTo(13f, 12.737f, 12.958f, 11f, 15.035f, 11.002f)
        close()
        moveTo(10.002f, 11.822f)
        curveTo(9.057f, 11.841f, 9f, 12.891f, 9f, 13.123f)
        lineTo(9f, 13.938f)
        curveTo(9f, 14.16f, 9.155f, 15.197f, 10.012f, 15.18f)
        curveTo(10.834f, 15.164f, 11f, 14.158f, 11f, 13.938f)
        lineTo(11f, 13.123f)
        curveTo(11f, 12.891f, 10.888f, 11.804f, 10.002f, 11.822f)
        close()
        moveTo(6f, 18f)
        lineTo(18f, 18f)
        lineTo(18f, 20f)
        lineTo(6f, 20f)
        lineTo(6f, 18f)
        close()
      }
    }.build()

    return _LogFile!!
  }

@Suppress("ObjectPropertyName")
private var _LogFile: ImageVector? = null

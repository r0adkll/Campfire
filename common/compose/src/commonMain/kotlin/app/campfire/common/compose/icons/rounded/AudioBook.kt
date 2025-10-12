package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.AudioBook: ImageVector
  get() {
    if (_AudioBook != null) {
      return _AudioBook!!
    }
    _AudioBook = ImageVector.Builder(
      name = "Rounded.AudioBook",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f,
    ).apply {
      path(fill = SolidColor(Color.Black)) {
        moveTo(3f, 4f)
        curveTo(2.448f, 4f, 2f, 4.448f, 2f, 5f)
        lineTo(2f, 18f)
        curveTo(2f, 19.105f, 2.895f, 20f, 4f, 20f)
        lineTo(10.277f, 20f)
        curveTo(10.624f, 20.596f, 11.261f, 21f, 12f, 21f)
        curveTo(12.739f, 21f, 13.376f, 20.596f, 13.723f, 20f)
        lineTo(20f, 20f)
        curveTo(21.105f, 20f, 22f, 19.105f, 22f, 18f)
        lineTo(22f, 5f)
        curveTo(22f, 4.448f, 21.552f, 4f, 21f, 4f)
        lineTo(14f, 4f)
        curveTo(13.228f, 4f, 12.532f, 4.3f, 12f, 4.779f)
        curveTo(11.468f, 4.3f, 10.772f, 4f, 10f, 4f)
        lineTo(3f, 4f)
        close()
        moveTo(12f, 8f)
        lineTo(14f, 8f)
        curveTo(14.552f, 8f, 15f, 8.448f, 15f, 9f)
        curveTo(15f, 9.552f, 14.552f, 10f, 14f, 10f)
        lineTo(13f, 10f)
        lineTo(13f, 13.893f)
        curveTo(13f, 14.889f, 12.319f, 15.813f, 11.336f, 15.973f)
        curveTo(10.083f, 16.176f, 9f, 15.215f, 9f, 14f)
        curveTo(9f, 12.895f, 9.895f, 12f, 11f, 12f)
        lineTo(11f, 9f)
        curveTo(11f, 8.448f, 11.448f, 8f, 12f, 8f)
        close()
      }
    }.build()

    return _AudioBook!!
  }

@Suppress("ObjectPropertyName")
private var _AudioBook: ImageVector? = null

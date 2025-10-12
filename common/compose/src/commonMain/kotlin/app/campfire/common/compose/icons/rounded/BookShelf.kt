package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.BookShelf: ImageVector
  get() {
    if (_BookShelf != null) {
      return _BookShelf!!
    }
    _BookShelf = ImageVector.Builder(
      name = "Rounded.BookShelf",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f,
    ).apply {
      path(fill = SolidColor(Color.Black)) {
        moveTo(5.5f, 3f)
        curveTo(4.683f, 3f, 4f, 3.683f, 4f, 4.5f)
        lineTo(4f, 17f)
        lineTo(2f, 17f)
        lineTo(2f, 19f)
        lineTo(4f, 19f)
        lineTo(4f, 21f)
        lineTo(6f, 21f)
        lineTo(6f, 19f)
        lineTo(8.5f, 19f)
        lineTo(18f, 19f)
        lineTo(18f, 21f)
        lineTo(20f, 21f)
        lineTo(20f, 19f)
        lineTo(22f, 19f)
        lineTo(22f, 17f)
        lineTo(20.145f, 17f)
        curveTo(20.161f, 16.727f, 20.102f, 16.49f, 20.051f, 16.293f)
        lineTo(17.803f, 7.213f)
        lineTo(17.801f, 7.207f)
        curveTo(17.726f, 6.917f, 17.702f, 6.527f, 17.201f, 6.158f)
        curveTo(16.951f, 5.974f, 16.631f, 5.906f, 16.406f, 5.906f)
        curveTo(16.182f, 5.906f, 16.015f, 5.944f, 15.844f, 5.986f)
        lineTo(14f, 6.434f)
        lineTo(14f, 4.5f)
        curveTo(14f, 3.683f, 13.317f, 3f, 12.5f, 3f)
        lineTo(9.5f, 3f)
        curveTo(9.324f, 3f, 9.158f, 3.036f, 9f, 3.094f)
        curveTo(8.842f, 3.036f, 8.676f, 3f, 8.5f, 3f)
        lineTo(5.5f, 3f)
        close()
        moveTo(6f, 5f)
        lineTo(8f, 5f)
        lineTo(8f, 17f)
        lineTo(6f, 17f)
        lineTo(6f, 5f)
        close()
        moveTo(10f, 5f)
        lineTo(12f, 5f)
        lineTo(12f, 17f)
        lineTo(10f, 17f)
        lineTo(10f, 5f)
        close()
        moveTo(15.943f, 8.021f)
        lineTo(18.039f, 16.49f)
        lineTo(16f, 17f)
        lineTo(15.992f, 17f)
        lineTo(14.008f, 8.49f)
        lineTo(15.943f, 8.021f)
        close()
      }
    }.build()

    return _BookShelf!!
  }

@Suppress("ObjectPropertyName")
private var _BookShelf: ImageVector? = null

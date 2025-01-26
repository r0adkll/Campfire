package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.ShakeVeryLow: ImageVector
  get() {
    if (_ShakeVeryLow != null) {
      return _ShakeVeryLow!!
    }
    _ShakeVeryLow = ImageVector.Builder(
      name = "Rounded.ShakeVeryLow",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f,
    ).apply {
      path(fill = SolidColor(Color(0xFF000000))) {
        moveTo(8f, 22f)
        curveTo(7.45f, 22f, 6.979f, 21.804f, 6.588f, 21.413f)
        curveTo(6.196f, 21.021f, 6f, 20.55f, 6f, 20f)
        verticalLineTo(4f)
        curveTo(6f, 3.45f, 6.196f, 2.979f, 6.588f, 2.588f)
        curveTo(6.979f, 2.196f, 7.45f, 2f, 8f, 2f)
        horizontalLineTo(16f)
        curveTo(16.55f, 2f, 17.021f, 2.196f, 17.413f, 2.588f)
        curveTo(17.804f, 2.979f, 18f, 3.45f, 18f, 4f)
        verticalLineTo(20f)
        curveTo(18f, 20.55f, 17.804f, 21.021f, 17.413f, 21.413f)
        curveTo(17.021f, 21.804f, 16.55f, 22f, 16f, 22f)
        horizontalLineTo(8f)
        close()
        moveTo(16f, 19f)
        horizontalLineTo(8f)
        verticalLineTo(20f)
        horizontalLineTo(16f)
        verticalLineTo(19f)
        close()
        moveTo(8f, 5f)
        horizontalLineTo(16f)
        verticalLineTo(4f)
        horizontalLineTo(8f)
        verticalLineTo(5f)
        close()
        moveTo(8f, 17f)
        horizontalLineTo(16f)
        verticalLineTo(7f)
        horizontalLineTo(8f)
        verticalLineTo(17f)
        close()
      }
    }.build()

    return _ShakeVeryLow!!
  }

@Suppress("ObjectPropertyName")
private var _ShakeVeryLow: ImageVector? = null

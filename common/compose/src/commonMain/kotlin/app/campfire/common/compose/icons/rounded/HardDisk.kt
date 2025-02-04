package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.HardDisk: ImageVector
  get() {
    if (_HardDisk != null) {
      return _HardDisk!!
    }
    _HardDisk = ImageVector.Builder(
      name = "Rounded.HardDisk",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f,
    ).apply {
      path(fill = SolidColor(Color(0xFFE8EAED))) {
        moveTo(240f, 880f)
        quadToRelative(-33f, 0f, -56.5f, -23.5f)
        reflectiveQuadTo(160f, 800f)
        verticalLineToRelative(-640f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadTo(240f, 80f)
        horizontalLineToRelative(480f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadTo(800f, 160f)
        verticalLineToRelative(640f)
        quadToRelative(0f, 33f, -23.5f, 56.5f)
        reflectiveQuadTo(720f, 880f)
        lineTo(240f, 880f)
        close()
        moveTo(240f, 800f)
        horizontalLineToRelative(480f)
        verticalLineToRelative(-640f)
        lineTo(240f, 160f)
        verticalLineToRelative(640f)
        close()
        moveTo(360f, 720f)
        horizontalLineToRelative(240f)
        quadToRelative(17f, 0f, 28.5f, -11.5f)
        reflectiveQuadTo(640f, 680f)
        quadToRelative(0f, -17f, -11.5f, -28.5f)
        reflectiveQuadTo(600f, 640f)
        lineTo(360f, 640f)
        quadToRelative(-17f, 0f, -28.5f, 11.5f)
        reflectiveQuadTo(320f, 680f)
        quadToRelative(0f, 17f, 11.5f, 28.5f)
        reflectiveQuadTo(360f, 720f)
        close()
        moveTo(480f, 560f)
        quadToRelative(66f, 0f, 113f, -47f)
        reflectiveQuadToRelative(47f, -113f)
        quadToRelative(0f, -66f, -47f, -113f)
        reflectiveQuadToRelative(-113f, -47f)
        quadToRelative(-66f, 0f, -113f, 47f)
        reflectiveQuadToRelative(-47f, 113f)
        quadToRelative(0f, 66f, 47f, 113f)
        reflectiveQuadToRelative(113f, 47f)
        close()
        moveTo(480f, 440f)
        quadToRelative(-17f, 0f, -28.5f, -11.5f)
        reflectiveQuadTo(440f, 400f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(480f, 360f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(520f, 400f)
        quadToRelative(0f, 17f, -11.5f, 28.5f)
        reflectiveQuadTo(480f, 440f)
        close()
        moveTo(480f, 400f)
        close()
      }
    }.build()

    return _HardDisk!!
  }

@Suppress("ObjectPropertyName")
private var _HardDisk: ImageVector? = null

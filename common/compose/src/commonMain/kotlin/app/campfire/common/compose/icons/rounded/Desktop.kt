package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.Desktop: ImageVector
  get() {
    if (_Desktop != null) {
      return _Desktop!!
    }
    _Desktop = ImageVector.Builder(
      name = "Rounded.Desktop",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f,
    ).apply {
      path(fill = SolidColor(Color(0xFFE8EAED))) {
        moveTo(400f, 760f)
        verticalLineToRelative(-80f)
        lineTo(160f, 680f)
        quadToRelative(-33f, 0f, -56.5f, -23.5f)
        reflectiveQuadTo(80f, 600f)
        verticalLineToRelative(-400f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadTo(160f, 120f)
        horizontalLineToRelative(640f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadTo(880f, 200f)
        verticalLineToRelative(400f)
        quadToRelative(0f, 33f, -23.5f, 56.5f)
        reflectiveQuadTo(800f, 680f)
        lineTo(560f, 680f)
        verticalLineToRelative(80f)
        horizontalLineToRelative(40f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(640f, 800f)
        quadToRelative(0f, 17f, -11.5f, 28.5f)
        reflectiveQuadTo(600f, 840f)
        lineTo(360f, 840f)
        quadToRelative(-17f, 0f, -28.5f, -11.5f)
        reflectiveQuadTo(320f, 800f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(360f, 760f)
        horizontalLineToRelative(40f)
        close()
        moveTo(160f, 600f)
        horizontalLineToRelative(640f)
        verticalLineToRelative(-400f)
        lineTo(160f, 200f)
        verticalLineToRelative(400f)
        close()
        moveTo(160f, 600f)
        verticalLineToRelative(-400f)
        verticalLineToRelative(400f)
        close()
      }
    }.build()

    return _Desktop!!
  }

@Suppress("ObjectPropertyName")
private var _Desktop: ImageVector? = null

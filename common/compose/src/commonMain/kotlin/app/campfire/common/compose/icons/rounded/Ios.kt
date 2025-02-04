package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.iOS: ImageVector
  get() {
    if (_iOS != null) {
      return _iOS!!
    }
    _iOS = ImageVector.Builder(
      name = "Rounded.iOS",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f,
    ).apply {
      path(fill = SolidColor(Color(0xFFE8EAED))) {
        moveTo(200f, 680f)
        quadToRelative(-17f, 0f, -28.5f, -11.5f)
        reflectiveQuadTo(160f, 640f)
        verticalLineToRelative(-160f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(200f, 440f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(240f, 480f)
        verticalLineToRelative(160f)
        quadToRelative(0f, 17f, -11.5f, 28.5f)
        reflectiveQuadTo(200f, 680f)
        close()
        moveTo(360f, 680f)
        quadToRelative(-33f, 0f, -56.5f, -23.5f)
        reflectiveQuadTo(280f, 600f)
        verticalLineToRelative(-240f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadTo(360f, 280f)
        horizontalLineToRelative(80f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadTo(520f, 360f)
        verticalLineToRelative(240f)
        quadToRelative(0f, 33f, -23.5f, 56.5f)
        reflectiveQuadTo(440f, 680f)
        horizontalLineToRelative(-80f)
        close()
        moveTo(360f, 600f)
        horizontalLineToRelative(80f)
        verticalLineToRelative(-240f)
        horizontalLineToRelative(-80f)
        verticalLineToRelative(240f)
        close()
        moveTo(600f, 680f)
        quadToRelative(-17f, 0f, -28.5f, -11.5f)
        reflectiveQuadTo(560f, 640f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(600f, 600f)
        horizontalLineToRelative(120f)
        verticalLineToRelative(-80f)
        horizontalLineToRelative(-80f)
        quadToRelative(-33f, 0f, -56.5f, -23.5f)
        reflectiveQuadTo(560f, 440f)
        verticalLineToRelative(-80f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadTo(640f, 280f)
        horizontalLineToRelative(120f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(800f, 320f)
        quadToRelative(0f, 17f, -11.5f, 28.5f)
        reflectiveQuadTo(760f, 360f)
        lineTo(640f, 360f)
        verticalLineToRelative(80f)
        horizontalLineToRelative(80f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadTo(800f, 520f)
        verticalLineToRelative(80f)
        quadToRelative(0f, 33f, -23.5f, 56.5f)
        reflectiveQuadTo(720f, 680f)
        lineTo(600f, 680f)
        close()
        moveTo(200f, 360f)
        quadToRelative(-17f, 0f, -28.5f, -11.5f)
        reflectiveQuadTo(160f, 320f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(200f, 280f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(240f, 320f)
        quadToRelative(0f, 17f, -11.5f, 28.5f)
        reflectiveQuadTo(200f, 360f)
        close()
      }
    }.build()

    return _iOS!!
  }

@Suppress("ObjectPropertyName")
private var _iOS: ImageVector? = null

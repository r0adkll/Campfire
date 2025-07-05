package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.Equalizer: ImageVector
  get() {
    if (_Equalizer != null) {
      return _Equalizer!!
    }
    _Equalizer = ImageVector.Builder(
      name = "Rounded.Equalizer",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f,
    ).apply {
      path(fill = SolidColor(Color(0xFFE8EAED))) {
        moveTo(240f, 800f)
        quadToRelative(-33f, 0f, -56.5f, -23.5f)
        reflectiveQuadTo(160f, 720f)
        verticalLineToRelative(-160f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadTo(240f, 480f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadTo(320f, 560f)
        verticalLineToRelative(160f)
        quadToRelative(0f, 33f, -23.5f, 56.5f)
        reflectiveQuadTo(240f, 800f)
        close()
        moveTo(480f, 800f)
        quadToRelative(-33f, 0f, -56.5f, -23.5f)
        reflectiveQuadTo(400f, 720f)
        verticalLineToRelative(-480f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadTo(480f, 160f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadTo(560f, 240f)
        verticalLineToRelative(480f)
        quadToRelative(0f, 33f, -23.5f, 56.5f)
        reflectiveQuadTo(480f, 800f)
        close()
        moveTo(720f, 800f)
        quadToRelative(-33f, 0f, -56.5f, -23.5f)
        reflectiveQuadTo(640f, 720f)
        verticalLineToRelative(-280f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadTo(720f, 360f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadTo(800f, 440f)
        verticalLineToRelative(280f)
        quadToRelative(0f, 33f, -23.5f, 56.5f)
        reflectiveQuadTo(720f, 800f)
        close()
      }
    }.build()

    return _Equalizer!!
  }

@Suppress("ObjectPropertyName")
private var _Equalizer: ImageVector? = null

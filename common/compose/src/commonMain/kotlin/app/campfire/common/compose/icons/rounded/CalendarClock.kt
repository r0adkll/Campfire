package app.campfire.common.compose.icons.rounded

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Rounded.CalendarClock: ImageVector
  get() {
    if (_CalendarClock != null) {
      return _CalendarClock!!
    }
    _CalendarClock = ImageVector.Builder(
      name = "CalendarClock",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f,
    ).apply {
      path(fill = SolidColor(Color(0xFFE8EAED))) {
        moveTo(200f, 320f)
        horizontalLineToRelative(560f)
        verticalLineToRelative(-80f)
        lineTo(200f, 240f)
        verticalLineToRelative(80f)
        close()
        moveTo(200f, 320f)
        verticalLineToRelative(-80f)
        verticalLineToRelative(80f)
        close()
        moveTo(200f, 880f)
        quadToRelative(-33f, 0f, -56.5f, -23.5f)
        reflectiveQuadTo(120f, 800f)
        verticalLineToRelative(-560f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadTo(200f, 160f)
        horizontalLineToRelative(40f)
        verticalLineToRelative(-40f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(280f, 80f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(320f, 120f)
        verticalLineToRelative(40f)
        horizontalLineToRelative(320f)
        verticalLineToRelative(-40f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(680f, 80f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(720f, 120f)
        verticalLineToRelative(40f)
        horizontalLineToRelative(40f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadTo(840f, 240f)
        verticalLineToRelative(187f)
        quadToRelative(0f, 17f, -11.5f, 28.5f)
        reflectiveQuadTo(800f, 467f)
        quadToRelative(-17f, 0f, -28.5f, -11.5f)
        reflectiveQuadTo(760f, 427f)
        verticalLineToRelative(-27f)
        lineTo(200f, 400f)
        verticalLineToRelative(400f)
        horizontalLineToRelative(232f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(472f, 840f)
        quadToRelative(0f, 17f, -11.5f, 28.5f)
        reflectiveQuadTo(432f, 880f)
        lineTo(200f, 880f)
        close()
        moveTo(720f, 920f)
        quadToRelative(-83f, 0f, -141.5f, -58.5f)
        reflectiveQuadTo(520f, 720f)
        quadToRelative(0f, -83f, 58.5f, -141.5f)
        reflectiveQuadTo(720f, 520f)
        quadToRelative(83f, 0f, 141.5f, 58.5f)
        reflectiveQuadTo(920f, 720f)
        quadToRelative(0f, 83f, -58.5f, 141.5f)
        reflectiveQuadTo(720f, 920f)
        close()
        moveTo(740f, 712f)
        verticalLineToRelative(-92f)
        quadToRelative(0f, -8f, -6f, -14f)
        reflectiveQuadToRelative(-14f, -6f)
        quadToRelative(-8f, 0f, -14f, 6f)
        reflectiveQuadToRelative(-6f, 14f)
        verticalLineToRelative(91f)
        quadToRelative(0f, 8f, 3f, 15.5f)
        reflectiveQuadToRelative(9f, 13.5f)
        lineToRelative(61f, 61f)
        quadToRelative(6f, 6f, 14f, 6f)
        reflectiveQuadToRelative(14f, -6f)
        quadToRelative(6f, -6f, 6f, -14f)
        reflectiveQuadToRelative(-6f, -14f)
        lineToRelative(-61f, -61f)
        close()
      }
    }.build()

    return _CalendarClock!!
  }

@Suppress("ObjectPropertyName")
private var _CalendarClock: ImageVector? = null

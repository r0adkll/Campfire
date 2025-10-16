package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.Crash: ImageVector
  get() {
    if (_Crash != null) {
      return _Crash!!
    }
    _Crash = ImageVector.Builder(
      name = "Rounded.Crash",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f,
    ).apply {
      path(fill = SolidColor(Color(0xFFE8EAED))) {
        moveTo(200f, 880f)
        quadToRelative(-33f, 0f, -56.5f, -23.5f)
        reflectiveQuadTo(120f, 800f)
        verticalLineToRelative(-160f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadTo(200f, 560f)
        horizontalLineToRelative(560f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadTo(840f, 640f)
        verticalLineToRelative(160f)
        quadToRelative(0f, 33f, -23.5f, 56.5f)
        reflectiveQuadTo(760f, 880f)
        lineTo(200f, 880f)
        close()
        moveTo(200f, 800f)
        horizontalLineToRelative(560f)
        verticalLineToRelative(-160f)
        lineTo(200f, 640f)
        verticalLineToRelative(160f)
        close()
        moveTo(434f, 252f)
        lineTo(512f, 114f)
        quadToRelative(5f, -9f, 14f, -13f)
        reflectiveQuadToRelative(18f, -2f)
        quadToRelative(9f, 2f, 15.5f, 7.5f)
        reflectiveQuadTo(568f, 123f)
        lineToRelative(26f, 157f)
        lineToRelative(154f, -42f)
        quadToRelative(11f, -3f, 19.5f, 0f)
        reflectiveQuadToRelative(13.5f, 11f)
        quadToRelative(5f, 8f, 5f, 17f)
        reflectiveQuadToRelative(-6f, 18f)
        lineToRelative(-92f, 130f)
        lineToRelative(29f, 17f)
        quadToRelative(14f, 8f, 18.5f, 24f)
        reflectiveQuadToRelative(-3.5f, 30f)
        quadToRelative(-8f, 14f, -24f, 18.5f)
        reflectiveQuadToRelative(-30f, -3.5f)
        lineToRelative(-68f, -38f)
        quadToRelative(-16f, -9f, -20f, -26.5f)
        reflectiveQuadToRelative(7f, -31.5f)
        lineToRelative(37f, -52f)
        lineToRelative(-104f, 29f)
        lineToRelative(-18f, -106f)
        lineToRelative(-52f, 93f)
        lineToRelative(-88f, -62f)
        lineToRelative(29f, 104f)
        lineToRelative(-106f, 18f)
        lineToRelative(5f, 3f)
        quadToRelative(14f, 8f, 18.5f, 24f)
        reflectiveQuadToRelative(-3.5f, 30f)
        quadToRelative(-8f, 14f, -24f, 18.5f)
        reflectiveQuadToRelative(-30f, -3.5f)
        lineToRelative(-128f, -72f)
        quadToRelative(-9f, -5f, -12.5f, -14f)
        reflectiveQuadToRelative(-1.5f, -18f)
        quadToRelative(2f, -9f, 7.5f, -15.5f)
        reflectiveQuadTo(143f, 372f)
        lineToRelative(157f, -26f)
        lineToRelative(-42f, -154f)
        quadToRelative(-3f, -11f, 0f, -19.5f)
        reflectiveQuadToRelative(11f, -13.5f)
        quadToRelative(8f, -5f, 17f, -5f)
        reflectiveQuadToRelative(18f, 6f)
        lineToRelative(130f, 92f)
        close()
        moveTo(487f, 500f)
        close()
        moveTo(480f, 720f)
        close()
      }
    }.build()

    return _Crash!!
  }

@Suppress("ObjectPropertyName")
private var _Crash: ImageVector? = null

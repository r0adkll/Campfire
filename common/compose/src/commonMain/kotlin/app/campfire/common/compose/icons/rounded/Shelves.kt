package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.Shelves: ImageVector
  get() {
    if (_Shelves != null) {
      return _Shelves!!
    }
    _Shelves = ImageVector.Builder(
      name = "Rounded.Shelves",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f,
    ).apply {
      path(fill = SolidColor(Color(0xFFE8EAED))) {
        moveTo(160f, 920f)
        quadToRelative(-17f, 0f, -28.5f, -11.5f)
        reflectiveQuadTo(120f, 880f)
        verticalLineToRelative(-800f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(160f, 40f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(200f, 80f)
        verticalLineToRelative(40f)
        horizontalLineToRelative(560f)
        verticalLineToRelative(-40f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(800f, 40f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(840f, 80f)
        verticalLineToRelative(800f)
        quadToRelative(0f, 17f, -11.5f, 28.5f)
        reflectiveQuadTo(800f, 920f)
        quadToRelative(-17f, 0f, -28.5f, -11.5f)
        reflectiveQuadTo(760f, 880f)
        verticalLineToRelative(-40f)
        lineTo(200f, 840f)
        verticalLineToRelative(40f)
        quadToRelative(0f, 17f, -11.5f, 28.5f)
        reflectiveQuadTo(160f, 920f)
        close()
        moveTo(200f, 440f)
        horizontalLineToRelative(80f)
        verticalLineToRelative(-120f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(320f, 280f)
        horizontalLineToRelative(160f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(520f, 320f)
        verticalLineToRelative(120f)
        horizontalLineToRelative(240f)
        verticalLineToRelative(-240f)
        lineTo(200f, 200f)
        verticalLineToRelative(240f)
        close()
        moveTo(200f, 760f)
        horizontalLineToRelative(240f)
        verticalLineToRelative(-120f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(480f, 600f)
        horizontalLineToRelative(160f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(680f, 640f)
        verticalLineToRelative(120f)
        horizontalLineToRelative(80f)
        verticalLineToRelative(-240f)
        lineTo(200f, 520f)
        verticalLineToRelative(240f)
        close()
        moveTo(360f, 440f)
        horizontalLineToRelative(80f)
        verticalLineToRelative(-80f)
        horizontalLineToRelative(-80f)
        verticalLineToRelative(80f)
        close()
        moveTo(520f, 760f)
        horizontalLineToRelative(80f)
        verticalLineToRelative(-80f)
        horizontalLineToRelative(-80f)
        verticalLineToRelative(80f)
        close()
        moveTo(360f, 440f)
        horizontalLineToRelative(80f)
        horizontalLineToRelative(-80f)
        close()
        moveTo(520f, 760f)
        horizontalLineToRelative(80f)
        horizontalLineToRelative(-80f)
        close()
      }
    }.build()

    return _Shelves!!
  }

@Suppress("ObjectPropertyName")
private var _Shelves: ImageVector? = null

package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.Book: ImageVector
  get() {
    if (_Book != null) {
      return _Book!!
    }
    _Book = ImageVector.Builder(
      name = "Rounded.Book",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f,
    ).apply {
      path(fill = SolidColor(Color(0xFFE8EAED))) {
        moveTo(240f, 613f)
        quadToRelative(14f, -7f, 29f, -10f)
        reflectiveQuadToRelative(31f, -3f)
        horizontalLineToRelative(20f)
        verticalLineToRelative(-440f)
        horizontalLineToRelative(-20f)
        quadToRelative(-25f, 0f, -42.5f, 17.5f)
        reflectiveQuadTo(240f, 220f)
        verticalLineToRelative(393f)
        close()
        moveTo(400f, 600f)
        horizontalLineToRelative(320f)
        verticalLineToRelative(-440f)
        lineTo(400f, 160f)
        verticalLineToRelative(440f)
        close()
        moveTo(240f, 613f)
        verticalLineToRelative(-453f)
        verticalLineToRelative(453f)
        close()
        moveTo(300f, 880f)
        quadToRelative(-58f, 0f, -99f, -41f)
        reflectiveQuadToRelative(-41f, -99f)
        verticalLineToRelative(-520f)
        quadToRelative(0f, -58f, 41f, -99f)
        reflectiveQuadToRelative(99f, -41f)
        horizontalLineToRelative(420f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadTo(800f, 160f)
        verticalLineToRelative(501f)
        quadToRelative(0f, 8f, -6.5f, 14.5f)
        reflectiveQuadTo(770f, 690f)
        quadToRelative(-14f, 7f, -22f, 20f)
        reflectiveQuadToRelative(-8f, 30f)
        quadToRelative(0f, 17f, 8f, 30.5f)
        reflectiveQuadToRelative(22f, 19.5f)
        quadToRelative(14f, 6f, 22f, 16.5f)
        reflectiveQuadToRelative(8f, 22.5f)
        verticalLineToRelative(10f)
        quadToRelative(0f, 17f, -11.5f, 29f)
        reflectiveQuadTo(760f, 880f)
        lineTo(300f, 880f)
        close()
        moveTo(300f, 800f)
        horizontalLineToRelative(373f)
        quadToRelative(-6f, -14f, -9.5f, -28.5f)
        reflectiveQuadTo(660f, 740f)
        quadToRelative(0f, -16f, 3f, -31f)
        reflectiveQuadToRelative(10f, -29f)
        lineTo(300f, 680f)
        quadToRelative(-26f, 0f, -43f, 17.5f)
        reflectiveQuadTo(240f, 740f)
        quadToRelative(0f, 26f, 17f, 43f)
        reflectiveQuadToRelative(43f, 17f)
        close()
      }
    }.build()

    return _Book!!
  }

@Suppress("ObjectPropertyName")
private var _Book: ImageVector? = null

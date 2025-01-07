package app.campfire.common.compose.icons.rounded

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Rounded.Bookmarks: ImageVector
  get() {
    if (_Bookmarks != null) {
      return _Bookmarks!!
    }
    _Bookmarks = ImageVector.Builder(
      name = "Bookmarks",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f,
    ).apply {
      path(fill = SolidColor(Color(0xFFE8EAED))) {
        moveToRelative(400f, 760f)
        lineToRelative(-182f, 91f)
        quadToRelative(-20f, 10f, -39f, -1.5f)
        reflectiveQuadTo(160f, 815f)
        verticalLineToRelative(-495f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadTo(240f, 240f)
        horizontalLineToRelative(320f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadTo(640f, 320f)
        verticalLineToRelative(495f)
        quadToRelative(0f, 23f, -19f, 34.5f)
        reflectiveQuadToRelative(-39f, 1.5f)
        lineToRelative(-182f, -91f)
        close()
        moveTo(240f, 759f)
        lineTo(362f, 693f)
        quadToRelative(18f, -10f, 38f, -10f)
        reflectiveQuadToRelative(38f, 10f)
        lineToRelative(122f, 66f)
        verticalLineToRelative(-439f)
        lineTo(240f, 320f)
        verticalLineToRelative(439f)
        close()
        moveTo(760f, 720f)
        quadToRelative(-17f, 0f, -28.5f, -11.5f)
        reflectiveQuadTo(720f, 680f)
        verticalLineToRelative(-520f)
        lineTo(320f, 160f)
        quadToRelative(-17f, 0f, -28.5f, -11.5f)
        reflectiveQuadTo(280f, 120f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(320f, 80f)
        horizontalLineToRelative(400f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadTo(800f, 160f)
        verticalLineToRelative(520f)
        quadToRelative(0f, 17f, -11.5f, 28.5f)
        reflectiveQuadTo(760f, 720f)
        close()
        moveTo(240f, 320f)
        horizontalLineToRelative(320f)
        horizontalLineToRelative(-320f)
        close()
      }
    }.build()

    return _Bookmarks!!
  }

@Suppress("ObjectPropertyName")
private var _Bookmarks: ImageVector? = null

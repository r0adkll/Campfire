package app.campfire.common.compose.icons.rounded

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Rounded.BookmarkAdd: ImageVector
  get() {
    if (_BookmarkAdd != null) {
      return _BookmarkAdd!!
    }
    _BookmarkAdd = ImageVector.Builder(
      name = "BookmarkAdd",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f,
    ).apply {
      path(fill = SolidColor(Color(0xFFE8EAED))) {
        moveToRelative(480f, 720f)
        lineToRelative(-168f, 72f)
        quadToRelative(-40f, 17f, -76f, -6.5f)
        reflectiveQuadTo(200f, 719f)
        verticalLineToRelative(-519f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadTo(280f, 120f)
        horizontalLineToRelative(200f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(520f, 160f)
        quadToRelative(0f, 17f, -11.5f, 28.5f)
        reflectiveQuadTo(480f, 200f)
        lineTo(280f, 200f)
        verticalLineToRelative(518f)
        lineToRelative(200f, -86f)
        lineToRelative(200f, 86f)
        verticalLineToRelative(-238f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(720f, 440f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(760f, 480f)
        verticalLineToRelative(239f)
        quadToRelative(0f, 43f, -36f, 66.5f)
        reflectiveQuadToRelative(-76f, 6.5f)
        lineToRelative(-168f, -72f)
        close()
        moveTo(480f, 200f)
        lineTo(280f, 200f)
        horizontalLineToRelative(240f)
        horizontalLineToRelative(-40f)
        close()
        moveTo(680f, 280f)
        horizontalLineToRelative(-40f)
        quadToRelative(-17f, 0f, -28.5f, -11.5f)
        reflectiveQuadTo(600f, 240f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(640f, 200f)
        horizontalLineToRelative(40f)
        verticalLineToRelative(-40f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(720f, 120f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(760f, 160f)
        verticalLineToRelative(40f)
        horizontalLineToRelative(40f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(840f, 240f)
        quadToRelative(0f, 17f, -11.5f, 28.5f)
        reflectiveQuadTo(800f, 280f)
        horizontalLineToRelative(-40f)
        verticalLineToRelative(40f)
        quadToRelative(0f, 17f, -11.5f, 28.5f)
        reflectiveQuadTo(720f, 360f)
        quadToRelative(-17f, 0f, -28.5f, -11.5f)
        reflectiveQuadTo(680f, 320f)
        verticalLineToRelative(-40f)
        close()
      }
    }.build()

    return _BookmarkAdd!!
  }

@Suppress("ObjectPropertyName")
private var _BookmarkAdd: ImageVector? = null

package app.campfire.common.compose.icons.filled

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Filled.PersonAdd: ImageVector
  get() {
    if (_PersonAdd != null) {
      return _PersonAdd!!
    }
    _PersonAdd = ImageVector.Builder(
      name = "PersonAdd",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f,
    ).apply {
      path(fill = SolidColor(Color(0xFFE8EAED))) {
        moveTo(720f, 440f)
        horizontalLineToRelative(-80f)
        quadToRelative(-17f, 0f, -28.5f, -11.5f)
        reflectiveQuadTo(600f, 400f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(640f, 360f)
        horizontalLineToRelative(80f)
        verticalLineToRelative(-80f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(760f, 240f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(800f, 280f)
        verticalLineToRelative(80f)
        horizontalLineToRelative(80f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(920f, 400f)
        quadToRelative(0f, 17f, -11.5f, 28.5f)
        reflectiveQuadTo(880f, 440f)
        horizontalLineToRelative(-80f)
        verticalLineToRelative(80f)
        quadToRelative(0f, 17f, -11.5f, 28.5f)
        reflectiveQuadTo(760f, 560f)
        quadToRelative(-17f, 0f, -28.5f, -11.5f)
        reflectiveQuadTo(720f, 520f)
        verticalLineToRelative(-80f)
        close()
        moveTo(360f, 480f)
        quadToRelative(-66f, 0f, -113f, -47f)
        reflectiveQuadToRelative(-47f, -113f)
        quadToRelative(0f, -66f, 47f, -113f)
        reflectiveQuadToRelative(113f, -47f)
        quadToRelative(66f, 0f, 113f, 47f)
        reflectiveQuadToRelative(47f, 113f)
        quadToRelative(0f, 66f, -47f, 113f)
        reflectiveQuadToRelative(-113f, 47f)
        close()
        moveTo(40f, 720f)
        verticalLineToRelative(-32f)
        quadToRelative(0f, -34f, 17.5f, -62.5f)
        reflectiveQuadTo(104f, 582f)
        quadToRelative(62f, -31f, 126f, -46.5f)
        reflectiveQuadTo(360f, 520f)
        quadToRelative(66f, 0f, 130f, 15.5f)
        reflectiveQuadTo(616f, 582f)
        quadToRelative(29f, 15f, 46.5f, 43.5f)
        reflectiveQuadTo(680f, 688f)
        verticalLineToRelative(32f)
        quadToRelative(0f, 33f, -23.5f, 56.5f)
        reflectiveQuadTo(600f, 800f)
        lineTo(120f, 800f)
        quadToRelative(-33f, 0f, -56.5f, -23.5f)
        reflectiveQuadTo(40f, 720f)
        close()
      }
    }.build()

    return _PersonAdd!!
  }

@Suppress("ObjectPropertyName")
private var _PersonAdd: ImageVector? = null

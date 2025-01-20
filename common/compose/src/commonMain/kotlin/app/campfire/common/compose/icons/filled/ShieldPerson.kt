package app.campfire.common.compose.icons.filled

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Filled.ShieldPerson: ImageVector
  get() {
    if (_ShieldPerson != null) {
      return _ShieldPerson!!
    }
    _ShieldPerson = ImageVector.Builder(
      name = "ShieldPerson",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f,
    ).apply {
      path(fill = SolidColor(Color(0xFFE8EAED))) {
        moveTo(480f, 520f)
        quadToRelative(58f, 0f, 99f, -41f)
        reflectiveQuadToRelative(41f, -99f)
        quadToRelative(0f, -58f, -41f, -99f)
        reflectiveQuadToRelative(-99f, -41f)
        quadToRelative(-58f, 0f, -99f, 41f)
        reflectiveQuadToRelative(-41f, 99f)
        quadToRelative(0f, 58f, 41f, 99f)
        reflectiveQuadToRelative(99f, 41f)
        close()
        moveTo(480f, 796f)
        quadToRelative(59f, -19f, 104.5f, -59.5f)
        reflectiveQuadTo(664f, 645f)
        quadToRelative(-43f, -22f, -89.5f, -33.5f)
        reflectiveQuadTo(480f, 600f)
        quadToRelative(-48f, 0f, -94.5f, 11.5f)
        reflectiveQuadTo(296f, 645f)
        quadToRelative(34f, 51f, 79.5f, 91.5f)
        reflectiveQuadTo(480f, 796f)
        close()
        moveTo(480f, 876f)
        quadToRelative(-7f, 0f, -13f, -1f)
        reflectiveQuadToRelative(-12f, -3f)
        quadToRelative(-135f, -45f, -215f, -166.5f)
        reflectiveQuadTo(160f, 444f)
        verticalLineToRelative(-189f)
        quadToRelative(0f, -25f, 14.5f, -45f)
        reflectiveQuadToRelative(37.5f, -29f)
        lineToRelative(240f, -90f)
        quadToRelative(14f, -5f, 28f, -5f)
        reflectiveQuadToRelative(28f, 5f)
        lineToRelative(240f, 90f)
        quadToRelative(23f, 9f, 37.5f, 29f)
        reflectiveQuadToRelative(14.5f, 45f)
        verticalLineToRelative(189f)
        quadToRelative(0f, 140f, -80f, 261.5f)
        reflectiveQuadTo(505f, 872f)
        quadToRelative(-6f, 2f, -12f, 3f)
        reflectiveQuadToRelative(-13f, 1f)
        close()
      }
    }.build()

    return _ShieldPerson!!
  }

@Suppress("ObjectPropertyName")
private var _ShieldPerson: ImageVector? = null

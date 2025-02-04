package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.SquareFoot: ImageVector
  get() {
    if (_SquareFoot != null) {
      return _SquareFoot!!
    }
    _SquareFoot = ImageVector.Builder(
      name = "Rounded.SquareFoot",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f,
    ).apply {
      path(fill = SolidColor(Color(0xFFE8EAED))) {
        moveTo(208f, 840f)
        quadToRelative(-37f, 0f, -62.5f, -25.5f)
        reflectiveQuadTo(120f, 752f)
        verticalLineToRelative(-548f)
        quadToRelative(0f, -29f, 27f, -40.5f)
        reflectiveQuadToRelative(47f, 8.5f)
        lineToRelative(90f, 90f)
        lineToRelative(-54f, 54f)
        lineToRelative(28f, 28f)
        lineToRelative(54f, -54f)
        lineToRelative(104f, 104f)
        lineToRelative(-54f, 54f)
        lineToRelative(28f, 28f)
        lineToRelative(54f, -54f)
        lineToRelative(104f, 104f)
        lineToRelative(-54f, 54f)
        lineToRelative(28f, 28f)
        lineToRelative(54f, -54f)
        lineToRelative(104f, 104f)
        lineToRelative(-54f, 54f)
        lineToRelative(28f, 28f)
        lineToRelative(54f, -54f)
        lineToRelative(80f, 80f)
        quadToRelative(20f, 20f, 8.5f, 47f)
        reflectiveQuadTo(756f, 840f)
        lineTo(208f, 840f)
        close()
        moveTo(240f, 720f)
        horizontalLineToRelative(332f)
        lineTo(240f, 388f)
        verticalLineToRelative(332f)
        close()
      }
    }.build()

    return _SquareFoot!!
  }

@Suppress("ObjectPropertyName")
private var _SquareFoot: ImageVector? = null

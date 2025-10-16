package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.AreaChartFilled: ImageVector
  get() {
    if (_AreaChartFilled != null) {
      return _AreaChartFilled!!
    }
    _AreaChartFilled = ImageVector.Builder(
      name = "Rounded.AreaChartFilled",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f,
    ).apply {
      path(fill = SolidColor(Color(0xFFE8EAED))) {
        moveTo(840f, 640f)
        lineTo(529f, 397f)
        quadToRelative(-27f, -21f, -60.5f, -16.5f)
        reflectiveQuadTo(415f, 413f)
        lineToRelative(-86f, 118f)
        quadToRelative(-10f, 14f, -26.5f, 16.5f)
        reflectiveQuadTo(272f, 539f)
        lineTo(120f, 420f)
        verticalLineToRelative(-60f)
        quadToRelative(0f, -25f, 22f, -36f)
        reflectiveQuadToRelative(42f, 4f)
        lineToRelative(96f, 72f)
        lineToRelative(151f, -211f)
        quadToRelative(20f, -28f, 54f, -33f)
        reflectiveQuadToRelative(61f, 17f)
        lineToRelative(134f, 107f)
        horizontalLineToRelative(80f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadTo(840f, 360f)
        verticalLineToRelative(280f)
        close()
        moveTo(120f, 800f)
        verticalLineToRelative(-280f)
        lineToRelative(135f, 108f)
        quadToRelative(27f, 22f, 60.5f, 17f)
        reflectiveQuadToRelative(53.5f, -33f)
        lineToRelative(87f, -119f)
        quadToRelative(10f, -14f, 26.5f, -16.5f)
        reflectiveQuadTo(513f, 485f)
        lineToRelative(327f, 256f)
        verticalLineToRelative(59f)
        lineTo(120f, 800f)
        close()
      }
    }.build()

    return _AreaChartFilled!!
  }

@Suppress("ObjectPropertyName")
private var _AreaChartFilled: ImageVector? = null

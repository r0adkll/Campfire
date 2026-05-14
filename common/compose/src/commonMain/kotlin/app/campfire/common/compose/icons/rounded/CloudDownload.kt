package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.CloudDownload: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "CloudDownload",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(440f, 486f)
      verticalLineToRelative(-242f)
      quadToRelative(-76f, 14f, -118f, 73.5f)
      reflectiveQuadTo(280f, 440f)
      horizontalLineToRelative(-20f)
      quadToRelative(-58f, 0f, -99f, 41f)
      reflectiveQuadToRelative(-41f, 99f)
      quadToRelative(0f, 58f, 41f, 99f)
      reflectiveQuadToRelative(99f, 41f)
      horizontalLineToRelative(480f)
      quadToRelative(42f, 0f, 71f, -29f)
      reflectiveQuadToRelative(29f, -71f)
      quadToRelative(0f, -42f, -29f, -71f)
      reflectiveQuadToRelative(-71f, -29f)
      horizontalLineToRelative(-60f)
      verticalLineToRelative(-80f)
      quadToRelative(0f, -48f, -22f, -89.5f)
      reflectiveQuadTo(600f, 280f)
      verticalLineToRelative(-93f)
      quadToRelative(74f, 35f, 117f, 103.5f)
      reflectiveQuadTo(760f, 440f)
      quadToRelative(69f, 8f, 114.5f, 59.5f)
      reflectiveQuadTo(920f, 620f)
      quadToRelative(0f, 75f, -52.5f, 127.5f)
      reflectiveQuadTo(740f, 800f)
      lineTo(260f, 800f)
      quadToRelative(-91f, 0f, -155.5f, -63f)
      reflectiveQuadTo(40f, 583f)
      quadToRelative(0f, -78f, 47f, -139f)
      reflectiveQuadToRelative(123f, -78f)
      quadToRelative(17f, -72f, 85f, -137f)
      reflectiveQuadToRelative(145f, -65f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(520f, 244f)
      verticalLineToRelative(242f)
      lineToRelative(36f, -35f)
      quadToRelative(11f, -11f, 27.5f, -11f)
      reflectiveQuadToRelative(28.5f, 12f)
      quadToRelative(11f, 11f, 11f, 28f)
      reflectiveQuadToRelative(-11f, 28f)
      lineTo(508f, 612f)
      quadToRelative(-12f, 12f, -28f, 12f)
      reflectiveQuadToRelative(-28f, -12f)
      lineTo(348f, 508f)
      quadToRelative(-11f, -11f, -11.5f, -27.5f)
      reflectiveQuadTo(348f, 452f)
      quadToRelative(11f, -11f, 27.5f, -11.5f)
      reflectiveQuadTo(404f, 451f)
      lineToRelative(36f, 35f)
      close()
      moveTo(480f, 442f)
      close()
    }
  }.build()
}

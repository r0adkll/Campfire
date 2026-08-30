package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.LocalLibrary: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "LocalLibrary",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(120f, 686f)
      verticalLineToRelative(-286f)
      quadToRelative(0f, -32f, 23.5f, -54f)
      reflectiveQuadToRelative(55.5f, -20f)
      quadToRelative(79f, 12f, 150.5f, 46.5f)
      reflectiveQuadTo(480f, 462f)
      quadToRelative(59f, -55f, 130.5f, -89.5f)
      reflectiveQuadTo(761f, 326f)
      quadToRelative(32f, -2f, 55.5f, 20f)
      reflectiveQuadToRelative(23.5f, 54f)
      verticalLineToRelative(286f)
      quadToRelative(0f, 32f, -21f, 54.5f)
      reflectiveQuadTo(766f, 765f)
      quadToRelative(-64f, 10f, -124f, 33f)
      reflectiveQuadToRelative(-112f, 61f)
      quadToRelative(-11f, 9f, -23.5f, 13.5f)
      reflectiveQuadTo(480f, 877f)
      quadToRelative(-14f, 0f, -26.5f, -4.5f)
      reflectiveQuadTo(430f, 859f)
      quadToRelative(-52f, -38f, -112f, -61f)
      reflectiveQuadToRelative(-124f, -33f)
      quadToRelative(-32f, -2f, -53f, -24.5f)
      reflectiveQuadTo(120f, 686f)
      close()
      moveTo(367f, 313f)
      quadToRelative(-47f, -47f, -47f, -113f)
      reflectiveQuadToRelative(47f, -113f)
      quadToRelative(47f, -47f, 113f, -47f)
      reflectiveQuadToRelative(113f, 47f)
      quadToRelative(47f, 47f, 47f, 113f)
      reflectiveQuadToRelative(-47f, 113f)
      quadToRelative(-47f, 47f, -113f, 47f)
      reflectiveQuadToRelative(-113f, -47f)
      close()
    }
  }.build()
}

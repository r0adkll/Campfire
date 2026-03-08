package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.DeleteSweep: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "DeleteSweep",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(200f, 760f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(120f, 680f)
      verticalLineToRelative(-360f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(80f, 280f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(120f, 240f)
      horizontalLineToRelative(120f)
      verticalLineToRelative(-20f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(280f, 180f)
      horizontalLineToRelative(80f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(400f, 220f)
      verticalLineToRelative(20f)
      horizontalLineToRelative(120f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(560f, 280f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(520f, 320f)
      verticalLineToRelative(360f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(440f, 760f)
      lineTo(200f, 760f)
      close()
      moveTo(640f, 720f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(600f, 680f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(640f, 640f)
      horizontalLineToRelative(80f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(760f, 680f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(720f, 720f)
      horizontalLineToRelative(-80f)
      close()
      moveTo(640f, 560f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(600f, 520f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(640f, 480f)
      horizontalLineToRelative(160f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(840f, 520f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(800f, 560f)
      lineTo(640f, 560f)
      close()
      moveTo(640f, 400f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(600f, 360f)
      quadToRelative(0f, -17f, 11.5f, -28.5f)
      reflectiveQuadTo(640f, 320f)
      horizontalLineToRelative(200f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(880f, 360f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(840f, 400f)
      lineTo(640f, 400f)
      close()
      moveTo(200f, 320f)
      verticalLineToRelative(360f)
      horizontalLineToRelative(240f)
      verticalLineToRelative(-360f)
      lineTo(200f, 320f)
      close()
    }
  }.build()
}

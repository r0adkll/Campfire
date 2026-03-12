package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.KeyboardDoubleArrowDownSemiBold: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "KeyboardDoubleArrowDown",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveToRelative(480f, 637.96f)
      lineToRelative(146.96f, -146.53f)
      quadToRelative(14.95f, -14.95f, 36.82f, -15.17f)
      quadToRelative(21.87f, -0.22f, 37.26f, 15.17f)
      quadTo(716f, 506.39f, 716f, 528.48f)
      quadToRelative(0f, 22.09f, -14.96f, 37.04f)
      lineToRelative(-183.43f, 184f)
      quadToRelative(-7.7f, 7.7f, -17.52f, 11.61f)
      quadToRelative(-9.83f, 3.91f, -20.09f, 3.91f)
      reflectiveQuadToRelative(-20.09f, -3.91f)
      quadToRelative(-9.82f, -3.91f, -17.52f, -11.61f)
      lineToRelative(-183.43f, -184f)
      quadTo(244f, 550.57f, 243.78f, 528.7f)
      quadToRelative(-0.22f, -21.87f, 15.18f, -37.27f)
      quadToRelative(14.95f, -14.95f, 37.04f, -14.95f)
      reflectiveQuadToRelative(37.04f, 14.95f)
      lineTo(480f, 637.96f)
      close()
      moveTo(480f, 380.43f)
      lineTo(626.96f, 233.91f)
      quadToRelative(14.95f, -14.95f, 36.82f, -15.17f)
      quadToRelative(21.87f, -0.22f, 37.26f, 15.74f)
      quadTo(716f, 249.43f, 716f, 271.52f)
      reflectiveQuadToRelative(-14.96f, 37.05f)
      lineTo(517.61f, 492f)
      quadToRelative(-7.7f, 7.7f, -17.52f, 11.61f)
      quadToRelative(-9.83f, 3.91f, -20.09f, 3.91f)
      reflectiveQuadToRelative(-20.09f, -3.91f)
      quadToRelative(-9.82f, -3.91f, -17.52f, -11.61f)
      lineTo(258.96f, 308.57f)
      quadToRelative(-14.96f, -14.96f, -15.18f, -36.55f)
      quadToRelative(-0.22f, -21.59f, 15.18f, -37.54f)
      quadToRelative(14.95f, -15.52f, 37.04f, -15.52f)
      reflectiveQuadToRelative(37.61f, 15.52f)
      lineTo(480f, 380.43f)
      close()
    }
  }.build()
}

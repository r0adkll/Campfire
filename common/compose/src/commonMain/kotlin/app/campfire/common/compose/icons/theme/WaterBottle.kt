package app.campfire.common.compose.icons.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Theme.WaterBottle: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "Theme.WaterBottle",
    defaultWidth = 64.dp,
    defaultHeight = 64.dp,
    viewportWidth = 64f,
    viewportHeight = 64f,
  ).apply {
    path(fill = SolidColor(Color(0xFF008AA9))) {
      moveTo(25f, 12f)
      horizontalLineToRelative(14f)
      verticalLineToRelative(11f)
      horizontalLineToRelative(-14f)
      close()
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(12f, 61f)
      arcToRelative(20f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, 40f, 0f)
      arcToRelative(20f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, -40f, 0f)
      close()
    }
    path(fill = SolidColor(Color(0xFF37D0EE))) {
      moveTo(42f, 55f)
      horizontalLineTo(22f)
      curveToRelative(-2.761f, 0f, -5f, -2.239f, -5f, -5f)
      verticalLineTo(36f)
      curveToRelative(0f, -8.284f, 6.716f, -15f, 15f, -15f)
      horizontalLineToRelative(0f)
      curveToRelative(8.284f, 0f, 15f, 6.716f, 15f, 15f)
      verticalLineToRelative(14f)
      curveTo(47f, 52.761f, 44.761f, 55f, 42f, 55f)
      close()
    }
    path(fill = SolidColor(Color(0xFF9C34C2))) {
      moveTo(39f, 7f)
      lineTo(39f, 7f)
      curveToRelative(-1.105f, 0f, -2f, -0.896f, -2f, -2f)
      lineToRelative(0f, 0f)
      curveToRelative(0f, -2.761f, -2.239f, -5f, -5f, -5f)
      reflectiveCurveToRelative(-5f, 2.239f, -5f, 5f)
      lineToRelative(0f, 0f)
      curveToRelative(0f, 1.104f, -0.895f, 2f, -2f, 2f)
      horizontalLineToRelative(0f)
      horizontalLineToRelative(0f)
      curveToRelative(-1.657f, 0f, -3f, 1.343f, -3f, 3f)
      verticalLineToRelative(2f)
      curveToRelative(0f, 1.657f, 1.343f, 3f, 3f, 3f)
      horizontalLineToRelative(2f)
      horizontalLineToRelative(10f)
      horizontalLineToRelative(2f)
      curveToRelative(1.657f, 0f, 3f, -1.343f, 3f, -3f)
      verticalLineToRelative(-2f)
      curveTo(42f, 8.343f, 40.657f, 7f, 39f, 7f)
      close()
      moveTo(32f, 7f)
      curveToRelative(-1.105f, 0f, -2f, -0.896f, -2f, -2f)
      reflectiveCurveToRelative(0.895f, -2f, 2f, -2f)
      reflectiveCurveToRelative(2f, 0.896f, 2f, 2f)
      reflectiveCurveTo(33.105f, 7f, 32f, 7f)
      close()
    }
    path(
      fill = SolidColor(Color.White),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(18f, 41f)
      curveToRelative(2.761f, 0f, 5f, -2.238f, 5f, -5f)
      curveToRelative(0f, -3.712f, 2.045f, -7.101f, 5.338f, -8.842f)
      curveTo(29.976f, 26.292f, 31f, 24.591f, 31f, 22.738f)
      verticalLineTo(15f)
      curveToRelative(0f, -2.051f, -1.234f, -3.813f, -3f, -4.584f)
      verticalLineTo(10f)
      curveToRelative(0f, -1.426f, -0.604f, -2.705f, -1.561f, -3.615f)
      curveTo(26.075f, 6.763f, 25.566f, 7f, 25f, 7f)
      horizontalLineToRelative(0f)
      horizontalLineToRelative(0f)
      curveToRelative(-1.657f, 0f, -3f, 1.343f, -3f, 3f)
      verticalLineToRelative(2f)
      curveToRelative(0f, 1.657f, 1.343f, 3f, 3f, 3f)
      verticalLineToRelative(7.738f)
      curveToRelative(-4.756f, 2.516f, -8f, 7.507f, -8f, 13.262f)
      verticalLineToRelative(4.899f)
      curveTo(17.323f, 40.965f, 17.657f, 41f, 18f, 41f)
      close()
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(47f, 50f)
      verticalLineTo(36f)
      curveToRelative(0f, -0.335f, -0.013f, -0.667f, -0.035f, -0.997f)
      curveTo(44.22f, 35.023f, 42f, 37.25f, 42f, 40f)
      verticalLineToRelative(10f)
      horizontalLineToRelative(-9f)
      curveToRelative(-2.761f, 0f, -5f, 2.238f, -5f, 5f)
      horizontalLineToRelative(14f)
      curveTo(44.761f, 55f, 47f, 52.761f, 47f, 50f)
      close()
    }
    path(fill = SolidColor(Color.White)) {
      moveTo(21.5f, 36f)
      curveToRelative(-0.829f, 0f, -1.5f, -0.672f, -1.5f, -1.5f)
      curveToRelative(0f, -4.235f, 2.7f, -7.981f, 6.718f, -9.32f)
      curveToRelative(0.787f, -0.261f, 1.635f, 0.163f, 1.897f, 0.948f)
      curveToRelative(0.262f, 0.786f, -0.163f, 1.636f, -0.949f, 1.897f)
      curveTo(24.875f, 28.956f, 23f, 31.558f, 23f, 34.5f)
      curveTo(23f, 35.328f, 22.329f, 36f, 21.5f, 36f)
      close()
    }
  }.build()
}

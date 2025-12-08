package app.campfire.common.compose.icons.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Theme.Forest: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "Theme.Forest",
    defaultWidth = 64.dp,
    defaultHeight = 64.dp,
    viewportWidth = 64f,
    viewportHeight = 64f,
  ).apply {
    path(fill = SolidColor(Color(0xFFDA7200))) {
      moveTo(46f, 54f)
      horizontalLineToRelative(-4f)
      curveToRelative(-1.105f, 0f, -2f, -0.895f, -2f, -2f)
      verticalLineToRelative(-9f)
      horizontalLineToRelative(8f)
      verticalLineToRelative(9f)
      curveTo(48f, 53.105f, 47.105f, 54f, 46f, 54f)
      close()
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(12f, 61f)
      arcToRelative(19f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, 38f, 0f)
      arcToRelative(19f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, -38f, 0f)
      close()
    }
    path(fill = SolidColor(Color(0xFFDA7200))) {
      moveTo(21.5f, 52.5f)
      moveToRelative(-1.5f, 0f)
      arcToRelative(1.5f, 1.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 3f, 0f)
      arcToRelative(1.5f, 1.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, -3f, 0f)
    }
    path(fill = SolidColor(Color(0xFFDA7200))) {
      moveTo(34.5f, 52.5f)
      moveToRelative(-1.5f, 0f)
      arcToRelative(1.5f, 1.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 3f, 0f)
      arcToRelative(1.5f, 1.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, -3f, 0f)
    }
    path(fill = SolidColor(Color(0xFFDA7200))) {
      moveTo(31.855f, 49.678f)
      curveTo(30.7f, 48.945f, 30f, 47.671f, 30f, 46.302f)
      verticalLineTo(36f)
      horizontalLineToRelative(-3f)
      horizontalLineToRelative(-3f)
      verticalLineToRelative(10.302f)
      curveToRelative(0f, 1.369f, -0.7f, 2.643f, -1.855f, 3.377f)
      lineToRelative(-2.433f, 1.545f)
      lineTo(20.5f, 54f)
      horizontalLineTo(27f)
      horizontalLineToRelative(6.5f)
      lineToRelative(0.789f, -2.776f)
      lineTo(31.855f, 49.678f)
      close()
    }
    path(fill = SolidColor(Color(0xFF98C900))) {
      moveTo(44.593f, 26.456f)
      curveTo(45.48f, 25.012f, 46f, 23.319f, 46f, 21.5f)
      curveToRelative(0f, -5.141f, -4.087f, -9.318f, -9.188f, -9.484f)
      curveTo(35.877f, 6.889f, 31.397f, 3f, 26f, 3f)
      curveToRelative(-5.403f, 0f, -9.887f, 3.899f, -10.815f, 9.035f)
      curveTo(14.958f, 12.016f, 14.731f, 12f, 14.5f, 12f)
      curveTo(9.806f, 12f, 6f, 15.806f, 6f, 20.5f)
      curveToRelative(0f, 1.233f, 0.268f, 2.401f, 0.74f, 3.458f)
      curveTo(4.47f, 25.694f, 3f, 28.422f, 3f, 31.5f)
      curveToRelative(0f, 5.247f, 4.253f, 9.5f, 9.5f, 9.5f)
      curveToRelative(1.615f, 0f, 3.135f, -0.406f, 4.467f, -1.117f)
      curveTo(19.167f, 42.401f, 22.393f, 44f, 26f, 44f)
      curveToRelative(3.785f, 0f, 7.156f, -1.757f, 9.355f, -4.495f)
      curveTo(36.666f, 40.443f, 38.266f, 41f, 40f, 41f)
      curveToRelative(4.418f, 0f, 8f, -3.582f, 8f, -8f)
      curveTo(48f, 30.293f, 46.652f, 27.904f, 44.593f, 26.456f)
      close()
    }
    path(
      fill = SolidColor(Color.White),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(30.87f, 4.14f)
      curveTo(30.35f, 6.35f, 28.37f, 8f, 26f, 8f)
      curveToRelative(-2.9f, 0f, -5.38f, 2.07f, -5.89f, 4.92f)
      curveToRelative(-0.46f, 2.53f, -2.76f, 4.29f, -5.31f, 4.1f)
      curveToRelative(-1.45f, -0.12f, -2.88f, 0.7f, -3.49f, 2.04f)
      curveTo(10.47f, 20.91f, 8.65f, 22f, 6.75f, 22f)
      curveToRelative(-0.21f, 0f, -0.41f, -0.01f, -0.62f, -0.05f)
      curveTo(6.04f, 21.48f, 6f, 20.99f, 6f, 20.5f)
      curveToRelative(0f, -4.69f, 3.81f, -8.5f, 8.5f, -8.5f)
      curveToRelative(0.23f, 0f, 0.46f, 0.02f, 0.69f, 0.03f)
      curveTo(16.11f, 6.9f, 20.6f, 3f, 26f, 3f)
      curveTo(27.75f, 3f, 29.4f, 3.41f, 30.87f, 4.14f)
      close()
    }
    path(fill = SolidColor(Color.White)) {
      moveTo(14.5f, 17f)
      curveToRelative(-0.828f, 0f, -1.5f, -0.672f, -1.5f, -1.5f)
      reflectiveCurveToRelative(0.672f, -1.5f, 1.5f, -1.5f)
      curveToRelative(1.897f, 0f, 2.69f, -0.601f, 3.029f, -2.294f)
      curveToRelative(0.162f, -0.812f, 0.959f, -1.343f, 1.765f, -1.177f)
      curveToRelative(0.812f, 0.162f, 1.34f, 0.952f, 1.177f, 1.765f)
      curveTo(19.847f, 15.417f, 17.838f, 17f, 14.5f, 17f)
      close()
    }
    path(fill = SolidColor(Color(0xFF5E8700))) {
      moveTo(59.288f, 42.171f)
      lineToRelative(-6.969f, -9.064f)
      curveTo(51.971f, 32.655f, 52.294f, 32f, 52.864f, 32f)
      horizontalLineToRelative(1.075f)
      curveToRelative(1.659f, 0f, 2.597f, -1.904f, 1.586f, -3.219f)
      lineToRelative(-5.131f, -6.674f)
      curveTo(50.046f, 21.655f, 50.369f, 21f, 50.94f, 21f)
      horizontalLineToRelative(0f)
      curveToRelative(1.659f, 0f, 2.597f, -1.904f, 1.586f, -3.219f)
      lineToRelative(-6.147f, -7.995f)
      curveToRelative(-1.201f, -1.562f, -3.556f, -1.562f, -4.757f, 0f)
      lineToRelative(-6.147f, 7.995f)
      curveTo(34.464f, 19.096f, 35.401f, 21f, 37.06f, 21f)
      horizontalLineToRelative(0f)
      curveToRelative(0.571f, 0f, 0.893f, 0.655f, 0.545f, 1.107f)
      lineToRelative(-5.131f, 6.674f)
      curveTo(31.464f, 30.096f, 32.401f, 32f, 34.06f, 32f)
      horizontalLineToRelative(1.075f)
      curveToRelative(0.571f, 0f, 0.893f, 0.655f, 0.545f, 1.107f)
      lineToRelative(-6.969f, 9.064f)
      curveTo(27.195f, 44.144f, 28.602f, 47f, 31.091f, 47f)
      horizontalLineToRelative(25.819f)
      curveTo(59.398f, 47f, 60.805f, 44.144f, 59.288f, 42.171f)
      close()
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(59.288f, 42.171f)
      lineToRelative(-6.969f, -9.064f)
      curveTo(51.971f, 32.655f, 52.294f, 32f, 52.864f, 32f)
      horizontalLineToRelative(1.075f)
      curveToRelative(1.659f, 0f, 2.597f, -1.904f, 1.586f, -3.219f)
      lineToRelative(-5.131f, -6.674f)
      curveToRelative(-0.077f, -0.101f, -0.116f, -0.211f, -0.132f, -0.321f)
      curveToRelative(-0.26f, 0.135f, -0.515f, 0.283f, -0.755f, 0.468f)
      curveToRelative(-1.926f, 1.48f, -2.476f, 4.089f, -1.433f, 6.186f)
      curveToRelative(-0.533f, 0.478f, -0.98f, 1.062f, -1.311f, 1.732f)
      curveToRelative(-0.965f, 1.957f, -0.74f, 4.248f, 0.592f, 5.983f)
      lineTo(51.849f, 42f)
      horizontalLineTo(43f)
      curveToRelative(-2.761f, 0f, -5f, 2.238f, -5f, 5f)
      horizontalLineToRelative(18.909f)
      curveTo(59.398f, 47f, 60.805f, 44.145f, 59.288f, 42.171f)
      close()
    }
  }.build()
}

package app.campfire.common.compose.icons.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Theme.Cactus: ImageVector by lazy(LazyThreadSafetyMode.PUBLICATION) {
  ImageVector.Builder(
    name = "Theme.Cactus",
    defaultWidth = 64.dp,
    defaultHeight = 64.dp,
    viewportWidth = 64f,
    viewportHeight = 64f,
  ).apply {
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(15f, 61f)
      arcToRelative(17f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, 34f, 0f)
      arcToRelative(17f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, -34f, 0f)
      close()
    }
    path(fill = SolidColor(Color(0xFF98C900))) {
      moveTo(32f, 14f)
      moveToRelative(-10f, 0f)
      arcToRelative(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, 20f, 0f)
      arcToRelative(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, -20f, 0f)
    }
    path(fill = SolidColor(Color(0xFF98C900))) {
      moveTo(41.981f, 14.613f)
      lineToRelative(-1.982f, 35.192f)
      curveTo(39.867f, 52.159f, 37.919f, 54f, 35.562f, 54f)
      horizontalLineToRelative(-7.124f)
      curveToRelative(-2.358f, 0f, -4.305f, -1.841f, -4.437f, -4.195f)
      lineToRelative(-1.982f, -35.192f)
      horizontalLineTo(41.981f)
      close()
    }
    path(fill = SolidColor(Color(0xFF98C900))) {
      moveTo(14.5f, 23.5f)
      moveToRelative(-4.5f, 0f)
      arcToRelative(4.5f, 4.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 9f, 0f)
      arcToRelative(4.5f, 4.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, -9f, 0f)
    }
    path(fill = SolidColor(Color(0xFF98C900))) {
      moveTo(49.5f, 34.5f)
      moveToRelative(-4.5f, 0f)
      arcToRelative(4.5f, 4.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 9f, 0f)
      arcToRelative(4.5f, 4.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, -9f, 0f)
    }
    path(fill = SolidColor(Color(0xFF98C900))) {
      moveTo(19f, 23.5f)
      curveTo(19f, 29f, 20f, 35f, 24f, 35f)
      curveToRelative(2.25f, 2.25f, 0f, 5f, 0f, 5f)
      curveToRelative(-8f, 0f, -14f, -8f, -14f, -16.5f)
      curveTo(14f, 23.5f, 19f, 23.5f, 19f, 23.5f)
      close()
    }
    path(fill = SolidColor(Color(0xFF98C900))) {
      moveTo(45f, 34.5f)
      curveToRelative(0f, 3.5f, -3f, 5.5f, -6.5f, 5.5f)
      curveToRelative(-1.5f, 1.5f, 0f, 5f, 0f, 5f)
      curveTo(47f, 45f, 54f, 41f, 54f, 34.5f)
      curveTo(50.5f, 34.5f, 45f, 34.5f, 45f, 34.5f)
      close()
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(54f, 34.5f)
      curveToRelative(0f, 6.04f, -6.05f, 9.92f, -13.73f, 10.44f)
      lineTo(40f, 49.81f)
      curveTo(39.87f, 52.16f, 37.92f, 54f, 35.56f, 54f)
      horizontalLineToRelative(-7.12f)
      curveToRelative(-0.5f, 0f, -0.98f, -0.08f, -1.43f, -0.24f)
      curveTo(27.13f, 51.11f, 29.32f, 49f, 32f, 49f)
      horizontalLineToRelative(3.04f)
      lineToRelative(0.24f, -4.35f)
      curveToRelative(0.05f, -0.94f, 0.37f, -1.82f, 0.87f, -2.55f)
      curveToRelative(-0.42f, -0.8f, -0.63f, -1.71f, -0.58f, -2.63f)
      lineToRelative(0.44f, -7.75f)
      curveToRelative(0.15f, -2.76f, 2.51f, -4.87f, 5.27f, -4.71f)
      lineToRelative(-0.71f, 12.74f)
      curveTo(43.1f, 39.11f, 45f, 37.28f, 45f, 34.5f)
      curveToRelative(0f, -2.49f, 2.01f, -4.5f, 4.5f, -4.5f)
      reflectiveCurveTo(54f, 32.01f, 54f, 34.5f)
      close()
    }
    path(
      fill = SolidColor(Color.White),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(36.84f, 5.25f)
      curveTo(36.29f, 7.41f, 34.33f, 9f, 32f, 9f)
      curveToRelative(-2.76f, 0f, -5f, 2.24f, -5f, 5f)
      lineToRelative(0.02f, 0.3f)
      curveToRelative(0.01f, 0.13f, 0.01f, 0.26f, 0.01f, 0.38f)
      lineToRelative(0.45f, 8.04f)
      curveToRelative(0.16f, 2.76f, -1.95f, 5.12f, -4.71f, 5.27f)
      lineToRelative(-0.75f, -13.38f)
      horizontalLineToRelative(0.01f)
      curveTo(22.02f, 14.41f, 22f, 14.21f, 22f, 14f)
      curveToRelative(0f, -5.52f, 4.48f, -10f, 10f, -10f)
      curveTo(33.75f, 4f, 35.4f, 4.45f, 36.84f, 5.25f)
      close()
    }
    path(fill = SolidColor(Color.White)) {
      moveTo(25.501f, 15f)
      curveToRelative(-0.12f, 0f, -0.243f, -0.015f, -0.365f, -0.045f)
      curveToRelative(-0.804f, -0.201f, -1.292f, -1.016f, -1.091f, -1.819f)
      curveToRelative(1.28f, -5.12f, 5.386f, -6.664f, 7.091f, -7.091f)
      curveToRelative(0.806f, -0.202f, 1.618f, 0.289f, 1.819f, 1.091f)
      curveToRelative(0.201f, 0.804f, -0.288f, 1.618f, -1.091f, 1.819f)
      curveToRelative(-1.466f, 0.367f, -4.054f, 1.492f, -4.909f, 4.909f)
      curveTo(26.785f, 14.545f, 26.173f, 15f, 25.501f, 15f)
      close()
    }
  }.build()
}

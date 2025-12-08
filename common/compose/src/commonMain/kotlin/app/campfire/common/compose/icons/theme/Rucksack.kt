package app.campfire.common.compose.icons.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Theme.Rucksack: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "Theme.Rucksack",
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
      moveTo(13f, 61f)
      arcToRelative(19f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, 38f, 0f)
      arcToRelative(19f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, -38f, 0f)
      close()
    }
    path(fill = SolidColor(Color(0xFFFFA500))) {
      moveTo(17f, 41f)
      horizontalLineToRelative(-4f)
      curveToRelative(-1.657f, 0f, -3f, -1.343f, -3f, -3f)
      verticalLineToRelative(-6f)
      curveToRelative(0f, -1.657f, 1.343f, -3f, 3f, -3f)
      horizontalLineToRelative(4f)
      verticalLineTo(41f)
      close()
    }
    path(fill = SolidColor(Color(0xFFFFA500))) {
      moveTo(47f, 41f)
      horizontalLineToRelative(4f)
      curveToRelative(1.657f, 0f, 3f, -1.343f, 3f, -3f)
      verticalLineToRelative(-6f)
      curveToRelative(0f, -1.657f, -1.343f, -3f, -3f, -3f)
      horizontalLineToRelative(-4f)
      verticalLineTo(41f)
      close()
    }
    path(fill = SolidColor(Color(0xFFBD6300))) {
      moveTo(45f, 46f)
      horizontalLineTo(19f)
      curveToRelative(-2.761f, 0f, -5f, -2.239f, -5f, -5f)
      verticalLineTo(17f)
      curveToRelative(0f, -2.761f, 2.239f, -5f, 5f, -5f)
      horizontalLineToRelative(26f)
      curveToRelative(2.761f, 0f, 5f, 2.239f, 5f, 5f)
      verticalLineToRelative(24f)
      curveTo(50f, 43.761f, 47.761f, 46f, 45f, 46f)
      close()
    }
    path(fill = SolidColor(Color(0xFF548500))) {
      moveTo(49f, 54f)
      horizontalLineTo(15f)
      curveToRelative(-1.657f, 0f, -3f, -1.343f, -3f, -3f)
      verticalLineToRelative(-5f)
      curveToRelative(0f, -1.657f, 1.343f, -3f, 3f, -3f)
      horizontalLineToRelative(34f)
      curveToRelative(1.657f, 0f, 3f, 1.343f, 3f, 3f)
      verticalLineToRelative(5f)
      curveTo(52f, 52.657f, 50.657f, 54f, 49f, 54f)
      close()
    }
    path(
      stroke = SolidColor(Color(0xFFDA7200)),
      strokeLineWidth = 4f,
    ) {
      moveTo(37f, 16f)
      horizontalLineTo(27f)
      verticalLineToRelative(-6f)
      curveToRelative(0f, -1.105f, 0.895f, -2f, 2f, -2f)
      horizontalLineToRelative(6f)
      curveToRelative(1.105f, 0f, 2f, 0.895f, 2f, 2f)
      verticalLineTo(16f)
      close()
    }
    path(fill = SolidColor(Color(0xFFFFA500))) {
      moveTo(45f, 29f)
      horizontalLineTo(19f)
      curveToRelative(-2.761f, 0f, -5f, -2.239f, -5f, -5f)
      verticalLineToRelative(-7f)
      curveToRelative(0f, -2.761f, 2.239f, -5f, 5f, -5f)
      horizontalLineToRelative(26f)
      curveToRelative(2.761f, 0f, 5f, 2.239f, 5f, 5f)
      verticalLineToRelative(7f)
      curveTo(50f, 26.761f, 47.761f, 29f, 45f, 29f)
      close()
    }
    path(fill = SolidColor(Color(0xFFFFCE29))) {
      moveTo(23.5f, 33f)
      lineTo(23.5f, 33f)
      curveToRelative(-1.381f, 0f, -2.5f, -1.119f, -2.5f, -2.5f)
      verticalLineToRelative(-3f)
      curveToRelative(0f, -1.381f, 1.119f, -2.5f, 2.5f, -2.5f)
      horizontalLineToRelative(0f)
      curveToRelative(1.381f, 0f, 2.5f, 1.119f, 2.5f, 2.5f)
      verticalLineToRelative(3f)
      curveTo(26f, 31.881f, 24.881f, 33f, 23.5f, 33f)
      close()
    }
    path(fill = SolidColor(Color(0xFFFFCE29))) {
      moveTo(40.5f, 33f)
      lineTo(40.5f, 33f)
      curveToRelative(-1.381f, 0f, -2.5f, -1.119f, -2.5f, -2.5f)
      verticalLineToRelative(-3f)
      curveToRelative(0f, -1.381f, 1.119f, -2.5f, 2.5f, -2.5f)
      horizontalLineToRelative(0f)
      curveToRelative(1.381f, 0f, 2.5f, 1.119f, 2.5f, 2.5f)
      verticalLineToRelative(3f)
      curveTo(43f, 31.881f, 41.881f, 33f, 40.5f, 33f)
      close()
    }
    path(fill = SolidColor(Color(0xFFFFA500))) {
      moveTo(23.5f, 56f)
      lineTo(23.5f, 56f)
      curveToRelative(-1.381f, 0f, -2.5f, -1.119f, -2.5f, -2.5f)
      verticalLineTo(43f)
      horizontalLineToRelative(5f)
      verticalLineToRelative(10.5f)
      curveTo(26f, 54.881f, 24.881f, 56f, 23.5f, 56f)
      close()
    }
    path(fill = SolidColor(Color(0xFFFFA500))) {
      moveTo(40.5f, 56f)
      lineTo(40.5f, 56f)
      curveToRelative(-1.381f, 0f, -2.5f, -1.119f, -2.5f, -2.5f)
      verticalLineTo(43f)
      horizontalLineToRelative(5f)
      verticalLineToRelative(10.5f)
      curveTo(43f, 54.881f, 41.881f, 56f, 40.5f, 56f)
      close()
    }
    path(
      fill = SolidColor(Color.White),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(36f, 12f)
      horizontalLineTo(19f)
      curveToRelative(-2.761f, 0f, -5f, 2.239f, -5f, 5f)
      verticalLineToRelative(8f)
      curveToRelative(2.761f, 0f, 5f, -2.239f, 5f, -5f)
      verticalLineToRelative(-3f)
      horizontalLineToRelative(12f)
      curveTo(33.761f, 17f, 36f, 14.761f, 36f, 12f)
      close()
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(49.025f, 43.003f)
      curveTo(47.802f, 43.914f, 47f, 45.358f, 47f, 47f)
      verticalLineToRelative(2f)
      horizontalLineTo(35f)
      curveToRelative(-2.761f, 0f, -5f, 2.239f, -5f, 5f)
      horizontalLineToRelative(8.05f)
      curveToRelative(0.232f, 1.141f, 1.24f, 2f, 2.45f, 2f)
      reflectiveCurveToRelative(2.218f, -0.859f, 2.45f, -2f)
      horizontalLineTo(49f)
      curveToRelative(1.657f, 0f, 3f, -1.343f, 3f, -3f)
      verticalLineToRelative(-5f)
      curveTo(52f, 44.352f, 50.67f, 43.016f, 49.025f, 43.003f)
      close()
    }
    path(
      stroke = SolidColor(Color.White),
      strokeLineWidth = 3f,
      strokeLineCap = StrokeCap.Round,
    ) {
      moveTo(18.612f, 19.5f)
      verticalLineToRelative(-2.191f)
      curveToRelative(0f, -0.552f, 0.448f, -1f, 1f, -1f)
      horizontalLineTo(23.5f)
    }
  }.build()
}

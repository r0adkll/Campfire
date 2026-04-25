package app.campfire.common.compose.icons.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Theme.Waterfall: ImageVector by lazy(LazyThreadSafetyMode.PUBLICATION) {
  ImageVector.Builder(
    name = "Theme.Waterfall",
    defaultWidth = 64.dp,
    defaultHeight = 64.dp,
    viewportWidth = 64f,
    viewportHeight = 64f,
  ).apply {
    path(fill = SolidColor(Color(0xFF008AA9))) {
      moveTo(20f, 9f)
      horizontalLineToRelative(25f)
      verticalLineToRelative(31f)
      horizontalLineToRelative(-25f)
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
    path(fill = SolidColor(Color(0xFFFFA500))) {
      moveTo(23f, 48f)
      horizontalLineTo(9f)
      verticalLineTo(12f)
      curveToRelative(0f, -1.657f, 1.343f, -3f, 3f, -3f)
      horizontalLineToRelative(8f)
      curveToRelative(1.657f, 0f, 3f, 1.343f, 3f, 3f)
      verticalLineTo(48f)
      close()
    }
    path(fill = SolidColor(Color(0xFFFFA500))) {
      moveTo(55f, 47f)
      lineToRelative(-14f, 1f)
      verticalLineTo(12f)
      curveToRelative(0f, -1.657f, 1.343f, -3f, 3f, -3f)
      horizontalLineToRelative(8f)
      curveToRelative(1.657f, 0f, 3f, 1.343f, 3f, 3f)
      verticalLineTo(47f)
      close()
    }
    path(fill = SolidColor(Color(0xFFA0EFFE))) {
      moveTo(24f, 36f)
      moveToRelative(-3f, 0f)
      arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, 6f, 0f)
      arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, -6f, 0f)
    }
    path(fill = SolidColor(Color(0xFFA0EFFE))) {
      moveTo(19f, 37f)
      moveToRelative(-3f, 0f)
      arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, 6f, 0f)
      arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, -6f, 0f)
    }
    path(fill = SolidColor(Color(0xFFA0EFFE))) {
      moveTo(45f, 37f)
      moveToRelative(-3f, 0f)
      arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, 6f, 0f)
      arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, -6f, 0f)
    }
    path(fill = SolidColor(Color(0xFFA0EFFE))) {
      moveTo(28f, 35f)
      moveToRelative(-3f, 0f)
      arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, 6f, 0f)
      arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, -6f, 0f)
    }
    path(fill = SolidColor(Color(0xFFA0EFFE))) {
      moveTo(36f, 35f)
      moveToRelative(-3f, 0f)
      arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, 6f, 0f)
      arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, -6f, 0f)
    }
    path(fill = SolidColor(Color(0xFFA0EFFE))) {
      moveTo(32f, 37f)
      moveToRelative(-3f, 0f)
      arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, 6f, 0f)
      arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, -6f, 0f)
    }
    path(fill = SolidColor(Color(0xFFA0EFFE))) {
      moveTo(32f, 33f)
      moveToRelative(-4f, 0f)
      arcToRelative(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 8f, 0f)
      arcToRelative(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, -8f, 0f)
    }
    path(fill = SolidColor(Color(0xFFA0EFFE))) {
      moveTo(40f, 36f)
      moveToRelative(-3f, 0f)
      arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, 6f, 0f)
      arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, -6f, 0f)
    }
    path(fill = SolidColor(Color(0xFF37D0EE))) {
      moveTo(52.978f, 36f)
      curveToRelative(-2.612f, 0f, -3.826f, 2f, -6.649f, 2f)
      curveToRelative(-1.403f, 0f, -2.044f, -0.357f, -2.855f, -0.81f)
      curveToRelative(-2.236f, -1.565f, -6.41f, -1.557f, -8.631f, 0f)
      curveTo(34.002f, 37.659f, 33.393f, 37.998f, 32f, 38f)
      curveToRelative(-1.393f, -0.002f, -2.002f, -0.341f, -2.842f, -0.809f)
      curveToRelative(-2.231f, -1.562f, -6.399f, -1.562f, -8.631f, 0f)
      curveTo(19.716f, 37.642f, 19.075f, 38f, 17.672f, 38f)
      curveToRelative(-2.824f, 0f, -4.037f, -2f, -6.649f, -2f)
      curveTo(8.924f, 35.999f, 9f, 38.084f, 9f, 38.084f)
      lineTo(8.999f, 47f)
      curveToRelative(0f, 2.209f, 1.791f, 4f, 4f, 4f)
      lineToRelative(38f, 0.001f)
      curveToRelative(2.209f, 0f, 4f, -1.791f, 4f, -4f)
      lineTo(55f, 38.085f)
      curveTo(55f, 38.085f, 55.076f, 36.001f, 52.978f, 36f)
      close()
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(50f, 35f)
      lineToRelative(0f, 11f)
      horizontalLineToRelative(-8f)
      curveToRelative(-2.761f, 0f, -5f, 2.238f, -5f, 5f)
      lineToRelative(13.999f, 0f)
      curveToRelative(2.209f, 0f, 4f, -1.791f, 4f, -4f)
      verticalLineTo(47f)
      horizontalLineTo(55f)
      verticalLineTo(30f)
      curveTo(52.239f, 30f, 50f, 32.238f, 50f, 35f)
      close()
    }
    path(fill = SolidColor(Color(0xFFA0EFFE))) {
      moveTo(26f, 28f)
      moveToRelative(-1f, 0f)
      arcToRelative(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = true, 2f, 0f)
      arcToRelative(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = true, -2f, 0f)
    }
    path(fill = SolidColor(Color(0xFFA0EFFE))) {
      moveTo(48f, 32f)
      moveToRelative(-1f, 0f)
      arcToRelative(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = true, 2f, 0f)
      arcToRelative(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = true, -2f, 0f)
    }
    path(fill = SolidColor(Color(0xFFA0EFFE))) {
      moveTo(15.5f, 31.5f)
      moveToRelative(-1.5f, 0f)
      arcToRelative(1.5f, 1.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 3f, 0f)
      arcToRelative(1.5f, 1.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, -3f, 0f)
    }
    path(fill = SolidColor(Color(0xFFA0EFFE))) {
      moveTo(37.5f, 27.5f)
      moveToRelative(-1.5f, 0f)
      arcToRelative(1.5f, 1.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 3f, 0f)
      arcToRelative(1.5f, 1.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, -3f, 0f)
    }
    path(fill = SolidColor(Color(0xFFA0EFFE))) {
      moveTo(33f, 43f)
      moveToRelative(-2f, 0f)
      arcToRelative(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 4f, 0f)
      arcToRelative(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, -4f, 0f)
    }
    path(
      fill = SolidColor(Color.White),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(14f, 24f)
      verticalLineTo(14f)
      horizontalLineToRelative(8f)
      curveToRelative(2.761f, 0f, 5f, -2.238f, 5f, -5f)
      horizontalLineToRelative(-7f)
      horizontalLineToRelative(-8f)
      curveToRelative(-1.657f, 0f, -3f, 1.343f, -3f, 3f)
      verticalLineToRelative(17f)
      curveTo(11.761f, 29f, 14f, 26.762f, 14f, 24f)
      close()
    }
    path(fill = SolidColor(Color.White)) {
      moveTo(12.5f, 18f)
      curveToRelative(-0.829f, 0f, -1.5f, -0.672f, -1.5f, -1.5f)
      verticalLineToRelative(-4f)
      curveToRelative(0f, -0.828f, 0.671f, -1.5f, 1.5f, -1.5f)
      horizontalLineToRelative(4f)
      curveToRelative(0.829f, 0f, 1.5f, 0.672f, 1.5f, 1.5f)
      reflectiveCurveTo(17.329f, 14f, 16.5f, 14f)
      horizontalLineTo(14f)
      verticalLineToRelative(2.5f)
      curveTo(14f, 17.328f, 13.329f, 18f, 12.5f, 18f)
      close()
    }
  }.build()
}

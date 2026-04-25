package app.campfire.common.compose.icons.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Theme.Lantern: ImageVector by lazy(LazyThreadSafetyMode.PUBLICATION) {
  ImageVector.Builder(
    name = "Theme.Lantern",
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
      moveTo(12f, 61f)
      arcToRelative(20f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, 40f, 0f)
      arcToRelative(20f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, -40f, 0f)
      close()
    }
    path(fill = SolidColor(Color(0xFFA11AC9))) {
      moveTo(49f, 51f)
      horizontalLineTo(15f)
      curveToRelative(-1.105f, 0f, -2f, -0.895f, -2f, -2f)
      verticalLineToRelative(0f)
      curveToRelative(0f, -1.105f, 0.895f, -2f, 2f, -2f)
      horizontalLineToRelative(34f)
      curveToRelative(1.105f, 0f, 2f, 0.895f, 2f, 2f)
      verticalLineToRelative(0f)
      curveTo(51f, 50.105f, 50.105f, 51f, 49f, 51f)
      close()
    }
    path(fill = SolidColor(Color(0xFFA11AC9))) {
      moveTo(52f, 55f)
      horizontalLineTo(12f)
      curveToRelative(-1.105f, 0f, -2f, -0.895f, -2f, -2f)
      verticalLineToRelative(0f)
      curveToRelative(0f, -1.105f, 0.895f, -2f, 2f, -2f)
      horizontalLineToRelative(40f)
      curveToRelative(1.105f, 0f, 2f, 0.895f, 2f, 2f)
      verticalLineToRelative(0f)
      curveTo(54f, 54.105f, 53.105f, 55f, 52f, 55f)
      close()
    }
    path(fill = SolidColor(Color(0xFFA11AC9))) {
      moveTo(49f, 16f)
      horizontalLineToRelative(-1f)
      horizontalLineToRelative(-0.76f)
      curveTo(45.179f, 11.361f, 39.138f, 8f, 32f, 8f)
      reflectiveCurveToRelative(-13.179f, 3.361f, -15.24f, 8f)
      horizontalLineTo(16f)
      horizontalLineToRelative(-1f)
      curveToRelative(-1.105f, 0f, -2f, 0.895f, -2f, 2f)
      curveToRelative(0f, 1.105f, 0.895f, 2f, 2f, 2f)
      horizontalLineToRelative(1f)
      verticalLineToRelative(30f)
      horizontalLineToRelative(32f)
      verticalLineTo(20f)
      horizontalLineToRelative(1f)
      curveToRelative(1.105f, 0f, 2f, -0.895f, 2f, -2f)
      curveTo(51f, 16.895f, 50.105f, 16f, 49f, 16f)
      close()
    }
    path(
      fill = SolidColor(Color.White),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(21f, 31f)
      verticalLineTo(20f)
      curveToRelative(0f, -0.396f, -0.046f, -0.78f, -0.133f, -1.148f)
      curveToRelative(0.178f, -0.256f, 0.332f, -0.529f, 0.461f, -0.82f)
      curveToRelative(0.562f, -1.265f, 1.812f, -2.461f, 3.52f, -3.368f)
      curveToRelative(2.231f, -1.186f, 3.173f, -3.817f, 2.328f, -6.124f)
      curveTo(22.239f, 9.662f, 18.335f, 12.455f, 16.76f, 16f)
      horizontalLineTo(16f)
      horizontalLineToRelative(-1f)
      curveToRelative(-1.105f, 0f, -2f, 0.895f, -2f, 2f)
      curveToRelative(0f, 1.105f, 0.895f, 2f, 2f, 2f)
      horizontalLineToRelative(1f)
      verticalLineToRelative(16f)
      curveTo(18.762f, 36f, 21f, 33.762f, 21f, 31f)
      close()
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(43f, 25f)
      verticalLineToRelative(17f)
      curveToRelative(0f, 2.762f, 2.238f, 5f, 5f, 5f)
      verticalLineTo(20f)
      curveTo(45.238f, 20f, 43f, 22.238f, 43f, 25f)
      close()
    }
    path(
      stroke = SolidColor(Color(0xFFA11AC9)),
      strokeLineWidth = 2f,
    ) {
      moveTo(32f, 6f)
      moveToRelative(-3f, 0f)
      arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, 6f, 0f)
      arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, -6f, 0f)
    }
    path(fill = SolidColor(Color(0xFFFFCE29))) {
      moveTo(21f, 20f)
      horizontalLineToRelative(22f)
      verticalLineToRelative(27f)
      horizontalLineToRelative(-22f)
      close()
    }
    path(fill = SolidColor(Color(0xFFFD3C4F))) {
      moveTo(25f, 39.85f)
      curveToRelative(0f, -2.784f, 3.48f, -8.752f, 5.533f, -12.029f)
      curveToRelative(0.685f, -1.094f, 2.25f, -1.094f, 2.935f, 0f)
      curveTo(35.52f, 31.098f, 39f, 37.065f, 39f, 39.85f)
      curveToRelative(0f, 3.949f, -3.134f, 7.15f, -7f, 7.15f)
      reflectiveCurveTo(25f, 43.799f, 25f, 39.85f)
      close()
    }
    path(fill = SolidColor(Color(0xFFFFE691))) {
      moveTo(28f, 43.067f)
      curveToRelative(0f, -1.531f, 1.989f, -4.814f, 3.162f, -6.616f)
      curveToRelative(0.391f, -0.602f, 1.285f, -0.602f, 1.677f, 0f)
      curveTo(34.011f, 38.254f, 36f, 41.536f, 36f, 43.067f)
      curveTo(36f, 45.239f, 34.209f, 47f, 32f, 47f)
      reflectiveCurveTo(28f, 45.239f, 28f, 43.067f)
      close()
    }
    path(
      stroke = SolidColor(Color.White),
      strokeLineWidth = 3f,
      strokeLineCap = StrokeCap.Round,
      strokeLineJoin = StrokeJoin.Round,
    ) {
      moveTo(21.798f, 14.948f)
      curveToRelative(1.037f, -0.946f, 2.369f, -1.737f, 3.893f, -2.321f)
    }
  }.build()
}

package app.campfire.common.compose.icons.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Theme.Bicycle: ImageVector by lazy(LazyThreadSafetyMode.PUBLICATION) {
  ImageVector.Builder(
    name = "Theme.Bicycle",
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
      moveTo(9f, 61f)
      arcToRelative(23f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, 46f, 0f)
      arcToRelative(23f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, -46f, 0f)
      close()
    }
    path(fill = SolidColor(Color(0xFF9C34C2))) {
      moveTo(12f, 55f)
      curveTo(5.383f, 55f, 0f, 49.617f, 0f, 43f)
      reflectiveCurveToRelative(5.383f, -12f, 12f, -12f)
      reflectiveCurveToRelative(12f, 5.383f, 12f, 12f)
      reflectiveCurveTo(18.617f, 55f, 12f, 55f)
      close()
      moveTo(12f, 36f)
      curveToRelative(-3.859f, 0f, -7f, 3.141f, -7f, 7f)
      reflectiveCurveToRelative(3.141f, 7f, 7f, 7f)
      reflectiveCurveToRelative(7f, -3.141f, 7f, -7f)
      reflectiveCurveTo(15.859f, 36f, 12f, 36f)
      close()
    }
    path(fill = SolidColor(Color(0xFF9C34C2))) {
      moveTo(52f, 55f)
      curveToRelative(-6.617f, 0f, -12f, -5.383f, -12f, -12f)
      reflectiveCurveToRelative(5.383f, -12f, 12f, -12f)
      reflectiveCurveToRelative(12f, 5.383f, 12f, 12f)
      reflectiveCurveTo(58.617f, 55f, 52f, 55f)
      close()
      moveTo(52f, 36f)
      curveToRelative(-3.859f, 0f, -7f, 3.141f, -7f, 7f)
      reflectiveCurveToRelative(3.141f, 7f, 7f, 7f)
      reflectiveCurveToRelative(7f, -3.141f, 7f, -7f)
      reflectiveCurveTo(55.859f, 36f, 52f, 36f)
      close()
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(64f, 43f)
      curveToRelative(0f, -1.741f, -0.381f, -3.391f, -1.05f, -4.885f)
      curveTo(60.693f, 38.598f, 59f, 40.599f, 59f, 43f)
      curveToRelative(0f, 3.859f, -3.141f, 7f, -7f, 7f)
      curveToRelative(-2.401f, 0f, -4.402f, 1.693f, -4.885f, 3.95f)
      curveTo(48.609f, 54.619f, 50.259f, 55f, 52f, 55f)
      curveTo(58.617f, 55f, 64f, 49.617f, 64f, 43f)
      close()
    }
    path(fill = SolidColor(Color(0xFFFD3C4F))) {
      moveTo(52.5f, 16f)
      horizontalLineTo(51f)
      curveToRelative(-1.104f, 0f, -2f, 0.896f, -2f, 2f)
      reflectiveCurveToRelative(0.896f, 2f, 2f, 2f)
      horizontalLineToRelative(1.5f)
      curveToRelative(3.032f, 0f, 5.5f, -2.468f, 5.5f, -5.5f)
      reflectiveCurveTo(55.532f, 9f, 52.5f, 9f)
      horizontalLineToRelative(-9.305f)
      curveToRelative(-2.046f, 0f, -3.931f, 1.026f, -5.04f, 2.745f)
      curveToRelative(-1.11f, 1.719f, -1.271f, 3.859f, -0.414f, 5.756f)
      lineTo(41.284f, 25f)
      horizontalLineTo(23.385f)
      lineToRelative(-3.677f, -6.04f)
      curveToRelative(-0.574f, -0.943f, -1.805f, -1.245f, -2.748f, -0.668f)
      curveToRelative(-0.943f, 0.574f, -1.242f, 1.805f, -0.668f, 2.748f)
      lineToRelative(3.49f, 5.734f)
      lineTo(14.304f, 41.94f)
      curveToRelative(-0.385f, 0.616f, -0.405f, 1.394f, -0.053f, 2.029f)
      reflectiveCurveTo(15.273f, 45f, 16f, 45f)
      horizontalLineToRelative(15.977f)
      curveToRelative(0.008f, 0f, 0.015f, 0f, 0.021f, 0f)
      curveToRelative(0.058f, 0f, 0.113f, -0.002f, 0.17f, -0.007f)
      curveToRelative(0.001f, 0f, 0.002f, 0f, 0.002f, 0f)
      curveToRelative(0.101f, -0.009f, 0.199f, -0.025f, 0.295f, -0.048f)
      curveToRelative(0.407f, -0.098f, 0.786f, -0.322f, 1.071f, -0.665f)
      curveToRelative(0.024f, -0.029f, 0.048f, -0.059f, 0.071f, -0.09f)
      lineToRelative(10.272f, -13.697f)
      lineToRelative(6.312f, 13.361f)
      curveTo(50.533f, 44.577f, 51.252f, 45f, 52.001f, 45f)
      curveToRelative(0.286f, 0f, 0.577f, -0.062f, 0.854f, -0.191f)
      curveToRelative(0.999f, -0.472f, 1.426f, -1.664f, 0.954f, -2.663f)
      lineTo(41.373f, 15.823f)
      curveToRelative(-0.285f, -0.631f, -0.233f, -1.326f, 0.143f, -1.908f)
      curveTo(41.891f, 13.333f, 42.503f, 13f, 43.195f, 13f)
      horizontalLineTo(52.5f)
      curveToRelative(0.827f, 0f, 1.5f, 0.673f, 1.5f, 1.5f)
      reflectiveCurveTo(53.327f, 16f, 52.5f, 16f)
      close()
      moveTo(19f, 41f)
      lineToRelative(3.109f, -10.402f)
      lineTo(28.441f, 41f)
      horizontalLineTo(19f)
      close()
      moveTo(32.173f, 39.437f)
      lineTo(25.819f, 29f)
      horizontalLineTo(40f)
      lineTo(32.173f, 39.437f)
      close()
    }
    path(fill = SolidColor(Color(0xFF6F7B91))) {
      moveTo(23.5f, 21f)
      horizontalLineToRelative(-12f)
      curveTo(10.119f, 21f, 9f, 19.881f, 9f, 18.5f)
      verticalLineToRelative(0f)
      curveToRelative(0f, -1.381f, 1.119f, -2.5f, 2.5f, -2.5f)
      horizontalLineToRelative(12f)
      curveToRelative(1.381f, 0f, 2.5f, 1.119f, 2.5f, 2.5f)
      verticalLineToRelative(0f)
      curveTo(26f, 19.881f, 24.881f, 21f, 23.5f, 21f)
      close()
    }
    path(fill = SolidColor(Color(0xFF6F7B91))) {
      moveTo(47f, 16f)
      horizontalLineToRelative(6f)
      verticalLineToRelative(4f)
      horizontalLineToRelative(-6f)
      curveToRelative(-1.105f, 0f, -2f, -0.895f, -2f, -2f)
      verticalLineToRelative(0f)
      curveTo(45f, 16.895f, 45.895f, 16f, 47f, 16f)
      close()
    }
    path(
      fill = SolidColor(Color.White),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(11.642f, 21f)
      lineToRelative(4.63f, 0f)
      curveToRelative(0.008f, 0.013f, 0.011f, 0.027f, 0.019f, 0.04f)
      lineToRelative(0f, 0f)
      curveToRelative(1.497f, 2.459f, 4.167f, 3.96f, 7.046f, 3.96f)
      horizontalLineToRelative(0.047f)
      lineToRelative(-2.435f, -4f)
      lineToRelative(2.408f, 0f)
      curveToRelative(1.308f, 0f, 2.499f, -0.941f, 2.629f, -2.242f)
      curveTo(26.137f, 17.261f, 24.966f, 16f, 23.5f, 16f)
      horizontalLineToRelative(-12f)
      curveToRelative(-1.466f, 0f, -2.637f, 1.261f, -2.487f, 2.758f)
      curveTo(9.143f, 20.059f, 10.335f, 21f, 11.642f, 21f)
      close()
    }
    path(fill = SolidColor(Color.White)) {
      moveTo(19.5f, 20f)
      horizontalLineToRelative(-5f)
      curveToRelative(-0.828f, 0f, -1.5f, -0.672f, -1.5f, -1.5f)
      reflectiveCurveToRelative(0.672f, -1.5f, 1.5f, -1.5f)
      horizontalLineToRelative(5f)
      curveToRelative(0.828f, 0f, 1.5f, 0.672f, 1.5f, 1.5f)
      reflectiveCurveTo(20.328f, 20f, 19.5f, 20f)
      close()
    }
  }.build()
}

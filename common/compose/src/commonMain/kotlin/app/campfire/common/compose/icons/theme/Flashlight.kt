package app.campfire.common.compose.icons.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Theme.Flashlight: ImageVector by lazy(LazyThreadSafetyMode.PUBLICATION) {
  ImageVector.Builder(
    name = "Theme.Flashlight",
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
      moveTo(16.358f, 54.006f)
      lineToRelative(-6.364f, -6.364f)
      curveToRelative(-1.562f, -1.562f, -1.562f, -4.095f, 0f, -5.657f)
      lineToRelative(26.284f, -26.284f)
      lineToRelative(12.021f, 12.021f)
      lineTo(22.015f, 54.006f)
      curveTo(20.453f, 55.568f, 17.92f, 55.568f, 16.358f, 54.006f)
      close()
    }
    path(fill = SolidColor(Color(0xFFFFA500))) {
      moveTo(28.854f, 35.146f)
      lineTo(28.854f, 35.146f)
      curveToRelative(-5.663f, -5.663f, -5.663f, -14.844f, 0f, -20.506f)
      lineToRelative(3.889f, -3.889f)
      lineToRelative(20.506f, 20.506f)
      lineToRelative(-3.889f, 3.889f)
      curveTo(43.697f, 40.809f, 34.516f, 40.809f, 28.854f, 35.146f)
      close()
    }
    path(fill = SolidColor(Color(0xFF9C34C2))) {
      moveTo(52.895f, 32.318f)
      lineTo(31.682f, 11.105f)
      curveToRelative(-0.976f, -0.976f, -0.976f, -2.559f, 0f, -3.536f)
      lineToRelative(0f, 0f)
      curveToRelative(0.976f, -0.976f, 2.559f, -0.976f, 3.536f, 0f)
      lineToRelative(21.213f, 21.213f)
      curveToRelative(0.976f, 0.976f, 0.976f, 2.559f, 0f, 3.536f)
      lineToRelative(0f, 0f)
      curveTo(55.454f, 33.294f, 53.871f, 33.294f, 52.895f, 32.318f)
      close()
    }
    path(fill = SolidColor(Color(0xFFFD3C4F))) {
      moveTo(21.782f, 42.218f)
      lineTo(21.782f, 42.218f)
      curveToRelative(-1.367f, -1.367f, -1.367f, -3.583f, 0f, -4.95f)
      lineToRelative(4.243f, -4.243f)
      curveToRelative(1.367f, -1.367f, 3.583f, -1.367f, 4.95f, 0f)
      lineToRelative(0f, 0f)
      curveToRelative(1.367f, 1.367f, 1.367f, 3.583f, 0f, 4.95f)
      lineToRelative(-4.243f, 4.243f)
      curveTo(25.365f, 43.584f, 23.149f, 43.584f, 21.782f, 42.218f)
      close()
    }
    path(fill = SolidColor(Color(0xFF9C34C2))) {
      moveTo(24.257f, 39.743f)
      moveToRelative(-3.5f, 0f)
      arcToRelative(3.5f, 3.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 7f, 0f)
      arcToRelative(3.5f, 3.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, -7f, 0f)
    }
    path(
      fill = SolidColor(Color.White),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(31.682f, 7.569f)
      curveToRelative(-0.976f, 0.976f, -0.976f, 2.559f, 0f, 3.536f)
      lineToRelative(0.354f, 0.354f)
      lineToRelative(-3.182f, 3.182f)
      curveToRelative(-3.415f, 3.415f, -4.755f, 8.107f, -4.052f, 12.537f)
      lineTo(12.825f, 39.154f)
      curveToRelative(0f, 0f, 0f, 0f, 0f, 0f)
      curveToRelative(0.976f, 0.976f, 2.256f, 1.464f, 3.535f, 1.464f)
      reflectiveCurveToRelative(2.56f, -0.488f, 3.535f, -1.464f)
      lineToRelative(8.441f, -8.441f)
      curveToRelative(1.133f, -1.132f, 1.654f, -2.738f, 1.403f, -4.319f)
      curveToRelative(-0.485f, -3.054f, 0.48f, -6.049f, 2.648f, -8.218f)
      lineToRelative(3.182f, -3.182f)
      curveToRelative(0.001f, 0f, 0.001f, 0f, 0.001f, 0f)
      lineToRelative(2.718f, 2.718f)
      curveToRelative(1.951f, 1.952f, 5.119f, 1.952f, 7.07f, 0f)
      curveToRelative(0f, 0f, 0f, 0f, 0f, 0f)
      lineTo(35.218f, 7.569f)
      curveTo(34.241f, 6.593f, 32.658f, 6.593f, 31.682f, 7.569f)
      close()
    }
    path(
      stroke = SolidColor(Color.White),
      strokeLineWidth = 3f,
      strokeLineCap = StrokeCap.Round,
      strokeLineJoin = StrokeJoin.Round,
    ) {
      moveTo(29.056f, 21.859f)
      curveToRelative(0.482f, -1.608f, 1.358f, -3.123f, 2.626f, -4.391f)
      lineToRelative(1.061f, -1.06f)
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(45.6f, 31.828f)
      curveToRelative(-2.144f, 2.011f, -5.053f, 2.897f, -7.992f, 2.433f)
      curveToRelative(-1.582f, -0.25f, -3.188f, 0.271f, -4.319f, 1.402f)
      lineTo(15.86f, 53.09f)
      curveToRelative(-0.082f, 0.082f, -0.133f, 0.15f, -0.195f, 0.223f)
      lineToRelative(0.693f, 0.693f)
      curveToRelative(1.562f, 1.562f, 4.095f, 1.562f, 5.657f, 0f)
      lineToRelative(14.808f, -14.808f)
      curveToRelative(4.43f, 0.703f, 9.122f, -0.637f, 12.537f, -4.052f)
      lineToRelative(3.182f, -3.182f)
      lineToRelative(0.01f, -0.01f)
      curveTo(50.654f, 30.049f, 47.577f, 29.975f, 45.6f, 31.828f)
      close()
    }
  }.build()
}

package app.campfire.common.compose.icons.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Theme.Mountain: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "Theme.Mountain",
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
    path(fill = SolidColor(Color(0xFF9C34C2))) {
      moveTo(54.295f, 28.574f)
      curveToRelative(-5.769f, -5.033f, -10.376f, -11.162f, -12.973f, -14.967f)
      horizontalLineTo(22.678f)
      curveToRelative(-2.597f, 3.805f, -7.204f, 9.934f, -12.973f, 14.967f)
      curveTo(8.621f, 29.52f, 8f, 30.889f, 8f, 32.328f)
      verticalLineTo(39f)
      horizontalLineToRelative(48f)
      verticalLineToRelative(-6.672f)
      curveTo(56f, 30.889f, 55.379f, 29.52f, 54.295f, 28.574f)
      close()
    }
    path(fill = SolidColor(Color(0xFFFFA1AC))) {
      moveTo(30.689f, 14.44f)
      curveTo(31.036f, 14.179f, 31.592f, 14f, 32f, 14f)
      curveToRelative(0.408f, 0f, 0.964f, 0.179f, 1.311f, 0.44f)
      lineToRelative(2.653f, 2.17f)
      curveToRelative(0.748f, 0.561f, 1.78f, 0.51f, 2.472f, -0.122f)
      lineToRelative(2.893f, -2.869f)
      curveToRelative(-0.339f, -0.496f, -0.648f, -0.958f, -0.916f, -1.368f)
      curveTo(39.49f, 10.841f, 37.923f, 10f, 36.238f, 10f)
      horizontalLineToRelative(-8.476f)
      curveToRelative(-1.685f, 0f, -3.252f, 0.841f, -4.175f, 2.25f)
      curveToRelative(-0.269f, 0.41f, -0.577f, 0.872f, -0.916f, 1.368f)
      lineToRelative(2.893f, 2.869f)
      curveToRelative(0.692f, 0.632f, 1.724f, 0.683f, 2.472f, 0.122f)
      lineTo(30.689f, 14.44f)
      close()
    }
    path(fill = SolidColor(Color(0xFF548500))) {
      moveTo(8f, 37f)
      horizontalLineToRelative(48f)
      verticalLineToRelative(4f)
      horizontalLineToRelative(-48f)
      close()
    }
    path(fill = SolidColor(Color(0xFF98C900))) {
      moveTo(8f, 41f)
      horizontalLineToRelative(48f)
      verticalLineToRelative(6f)
      horizontalLineToRelative(-48f)
      close()
    }
    path(fill = SolidColor(Color(0xFF68E5FD))) {
      moveTo(51.718f, 44.199f)
      curveToRelative(-0.964f, 0.723f, -1.875f, 1.796f, -3.722f, 1.796f)
      curveToRelative(-1.846f, 0f, -2.757f, -0.683f, -3.722f, -1.406f)
      curveToRelative(-1.046f, -0.784f, -2.127f, -1.594f, -4.282f, -1.594f)
      curveToRelative(-2.154f, 0f, -3.234f, 0.811f, -4.279f, 1.594f)
      curveToRelative(-0.963f, 0.723f, -1.874f, 1.406f, -3.719f, 1.406f)
      curveToRelative(-1.845f, 0f, -2.756f, -0.683f, -3.72f, -1.406f)
      curveToRelative(-1.045f, -0.784f, -2.126f, -1.594f, -4.281f, -1.594f)
      curveToRelative(-2.154f, 0f, -3.235f, 0.811f, -4.279f, 1.594f)
      curveToRelative(-0.963f, 0.723f, -1.874f, 1.406f, -3.718f, 1.406f)
      curveToRelative(-1.844f, 0f, -2.754f, -1.074f, -3.717f, -1.798f)
      curveToRelative(-0.467f, -0.35f, -0.94f, -0.706f, -1.514f, -0.992f)
      curveTo(9.486f, 42.567f, 8f, 43.531f, 8f, 45.008f)
      verticalLineTo(53f)
      curveToRelative(0f, 1.105f, 0.86f, 2f, 1.92f, 2f)
      horizontalLineToRelative(44.16f)
      curveToRelative(1.06f, 0f, 1.92f, -0.895f, 1.92f, -2f)
      verticalLineToRelative(-7.992f)
      curveToRelative(0f, -1.477f, -1.485f, -2.44f, -2.764f, -1.803f)
      curveTo(52.661f, 43.492f, 52.186f, 43.848f, 51.718f, 44.199f)
      close()
    }
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(56f, 53f)
      verticalLineToRelative(-7.992f)
      curveToRelative(-0.003f, -0.003f, -0.005f, -0.005f, -0.008f, -0.008f)
      curveToRelative(-2.24f, 0.004f, -4.135f, 1.48f, -4.767f, 3.513f)
      curveTo(50.956f, 49.376f, 50.211f, 50f, 49.307f, 50f)
      horizontalLineTo(41f)
      curveToRelative(-2.762f, 0f, -5f, 2.239f, -5f, 5f)
      horizontalLineToRelative(18.08f)
      curveTo(55.14f, 55f, 56f, 54.105f, 56f, 53f)
      close()
    }
    path(
      fill = SolidColor(Color.White),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(9.707f, 33.574f)
      curveToRelative(1.166f, 0f, 2.337f, -0.405f, 3.285f, -1.232f)
      curveToRelative(3.444f, -3.004f, 6.863f, -6.674f, 10.16f, -10.908f)
      curveToRelative(1.266f, -1.624f, 2.496f, -3.309f, 3.656f, -5.008f)
      curveToRelative(1.341f, -1.964f, 1.083f, -4.539f, -0.469f, -6.212f)
      curveToRelative(-1.116f, 0.33f, -2.098f, 1.036f, -2.753f, 2.037f)
      curveToRelative(-0.267f, 0.407f, -0.573f, 0.865f, -0.909f, 1.357f)
      curveToRelative(-2.597f, 3.805f, -7.204f, 9.934f, -12.972f, 14.966f)
      curveTo(8.621f, 29.52f, 8f, 30.889f, 8f, 32.328f)
      verticalLineToRelative(0.921f)
      curveTo(8.551f, 33.449f, 9.125f, 33.574f, 9.707f, 33.574f)
      close()
    }
    path(
      stroke = SolidColor(Color.White),
      strokeLineWidth = 3f,
      strokeLineCap = StrokeCap.Round,
      strokeLineJoin = StrokeJoin.Round,
    ) {
      moveTo(20.34f, 22.55f)
      curveToRelative(0.86f, -1.042f, 1.665f, -2.065f, 2.408f, -3.047f)
    }
  }.build()
}

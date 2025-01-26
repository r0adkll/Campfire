package app.campfire.common.compose.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var _ParkBench: ImageVector? = null

val CampfireIcons.ParkBench: ImageVector
  get() {
    if (_ParkBench != null) {
      return _ParkBench!!
    }
    _ParkBench = ImageVector.Builder(
      name = "Icons8ParkBench",
      defaultWidth = 64.dp,
      defaultHeight = 64.dp,
      viewportWidth = 64f,
      viewportHeight = 64f,
    ).apply {
      group {
        path(
          fill = SolidColor(Color(0xFFDA7200)),
          fillAlpha = 1.0f,
          stroke = null,
          strokeAlpha = 1.0f,
          strokeLineWidth = 1.0f,
          strokeLineCap = StrokeCap.Butt,
          strokeLineJoin = StrokeJoin.Miter,
          strokeLineMiter = 1.0f,
          pathFillType = PathFillType.NonZero,
        ) {
          moveTo(22f, 53f)
          horizontalLineToRelative(-4f)
          curveToRelative(-1.105f, 0f, -2f, -0.895f, -2f, -2f)
          verticalLineToRelative(-9f)
          horizontalLineToRelative(8f)
          verticalLineToRelative(9f)
          curveTo(24f, 52.105f, 23.105f, 53f, 22f, 53f)
          close()
        }
      }
      group {
        path(
          fill = SolidColor(Color(0xFF5E8700)),
          fillAlpha = 1.0f,
          stroke = null,
          strokeAlpha = 1.0f,
          strokeLineWidth = 1.0f,
          strokeLineCap = StrokeCap.Butt,
          strokeLineJoin = StrokeJoin.Miter,
          strokeLineMiter = 1.0f,
          pathFillType = PathFillType.NonZero,
        ) {
          moveTo(35.288f, 41.171f)
          lineToRelative(-6.969f, -9.064f)
          curveTo(27.971f, 31.655f, 28.294f, 31f, 28.864f, 31f)
          horizontalLineToRelative(1.075f)
          curveToRelative(1.659f, 0f, 2.597f, -1.904f, 1.586f, -3.219f)
          lineToRelative(-5.131f, -6.674f)
          curveTo(26.046f, 20.655f, 26.369f, 20f, 26.94f, 20f)
          horizontalLineToRelative(0f)
          curveToRelative(1.659f, 0f, 2.597f, -1.904f, 1.586f, -3.219f)
          lineToRelative(-6.147f, -7.995f)
          curveToRelative(-1.201f, -1.562f, -3.556f, -1.562f, -4.757f, 0f)
          lineToRelative(-6.147f, 7.995f)
          curveTo(10.464f, 18.096f, 11.401f, 20f, 13.06f, 20f)
          horizontalLineToRelative(0f)
          curveToRelative(0.571f, 0f, 0.893f, 0.655f, 0.545f, 1.107f)
          lineToRelative(-5.131f, 6.674f)
          curveTo(7.464f, 29.096f, 8.401f, 31f, 10.06f, 31f)
          horizontalLineToRelative(1.075f)
          curveToRelative(0.571f, 0f, 0.893f, 0.655f, 0.545f, 1.107f)
          lineToRelative(-6.969f, 9.064f)
          curveTo(3.195f, 43.144f, 4.602f, 46f, 7.091f, 46f)
          horizontalLineToRelative(25.819f)
          curveTo(35.398f, 46f, 36.805f, 43.144f, 35.288f, 41.171f)
          close()
        }
      }
      group {
        path(
          fill = SolidColor(Color(0xFFFFFFFF)),
          fillAlpha = 0.3f,
          stroke = null,
          strokeAlpha = 1.0f,
          strokeLineWidth = 1.0f,
          strokeLineCap = StrokeCap.Butt,
          strokeLineJoin = StrokeJoin.Miter,
          strokeLineMiter = 1.0f,
          pathFillType = PathFillType.NonZero,
        ) {
          moveTo(11.496f, 32.348f)
          curveToRelative(0.741f, -0.327f, 1.415f, -0.833f, 1.943f, -1.519f)
          lineToRelative(5.134f, -6.678f)
          curveToRelative(1.328f, -1.731f, 1.554f, -4.022f, 0.589f, -5.979f)
          curveToRelative(-0.184f, -0.373f, -0.403f, -0.719f, -0.653f, -1.034f)
          lineToRelative(4.078f, -5.304f)
          curveToRelative(0.399f, -0.519f, 0.671f, -1.093f, 0.839f, -1.687f)
          lineToRelative(-1.047f, -1.361f)
          curveToRelative(-1.201f, -1.563f, -3.556f, -1.563f, -4.757f, 0f)
          lineToRelative(-6.147f, 7.995f)
          curveTo(10.464f, 18.096f, 11.401f, 20f, 13.06f, 20f)
          curveToRelative(0.571f, 0f, 0.893f, 0.655f, 0.545f, 1.107f)
          lineToRelative(-5.131f, 6.674f)
          curveTo(7.464f, 29.096f, 8.401f, 31f, 10.06f, 31f)
          horizontalLineToRelative(1.075f)
          curveToRelative(0.571f, 0f, 0.893f, 0.655f, 0.545f, 1.107f)
          lineTo(11.496f, 32.348f)
          close()
        }
      }
      group {
        path(
          fill = SolidColor(Color.Black),
          fillAlpha = 0.3f,
          stroke = null,
          strokeAlpha = 1.0f,
          strokeLineWidth = 1.0f,
          strokeLineCap = StrokeCap.Butt,
          strokeLineJoin = StrokeJoin.Miter,
          strokeLineMiter = 1.0f,
          pathFillType = PathFillType.NonZero,
        ) {
          moveTo(52f, 61f)
          arcTo(20f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 32f, 64f)
          arcTo(20f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 12f, 61f)
          arcTo(20f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 52f, 61f)
          close()
        }
      }
      group {
        path(
          fill = SolidColor(Color(0xFFA7B3C7)),
          fillAlpha = 1.0f,
          stroke = null,
          strokeAlpha = 1.0f,
          strokeLineWidth = 1.0f,
          strokeLineCap = StrokeCap.Butt,
          strokeLineJoin = StrokeJoin.Miter,
          strokeLineMiter = 1.0f,
          pathFillType = PathFillType.NonZero,
        ) {
          moveTo(40.502f, 53f)
          curveToRelative(-0.098f, 0f, -0.197f, -0.009f, -0.296f, -0.029f)
          curveToRelative(-0.813f, -0.162f, -1.34f, -0.952f, -1.177f, -1.765f)
          lineToRelative(1.5f, -7.5f)
          curveToRelative(0.162f, -0.812f, 0.962f, -1.344f, 1.765f, -1.177f)
          curveToRelative(0.813f, 0.162f, 1.34f, 0.952f, 1.177f, 1.765f)
          lineToRelative(-1.5f, 7.5f)
          curveTo(41.828f, 52.507f, 41.202f, 53f, 40.502f, 53f)
          close()
        }
      }
      group {
        path(
          fill = SolidColor(Color(0xFFA7B3C7)),
          fillAlpha = 1.0f,
          stroke = null,
          strokeAlpha = 1.0f,
          strokeLineWidth = 1.0f,
          strokeLineCap = StrokeCap.Butt,
          strokeLineJoin = StrokeJoin.Miter,
          strokeLineMiter = 1.0f,
          pathFillType = PathFillType.NonZero,
        ) {
          moveTo(58.498f, 53f)
          curveToRelative(-0.754f, 0f, -1.403f, -0.567f, -1.489f, -1.334f)
          lineToRelative(-1f, -9f)
          curveToRelative(-0.091f, -0.823f, 0.502f, -1.565f, 1.325f, -1.657f)
          curveToRelative(0.826f, -0.089f, 1.564f, 0.501f, 1.657f, 1.325f)
          lineToRelative(1f, 9f)
          curveToRelative(0.091f, 0.823f, -0.502f, 1.565f, -1.325f, 1.657f)
          curveTo(58.609f, 52.997f, 58.554f, 53f, 58.498f, 53f)
          close()
        }
      }
      group {
        path(
          fill = SolidColor(Color(0xFFFFA500)),
          fillAlpha = 1.0f,
          stroke = null,
          strokeAlpha = 1.0f,
          strokeLineWidth = 1.0f,
          strokeLineCap = StrokeCap.Butt,
          strokeLineJoin = StrokeJoin.Miter,
          strokeLineMiter = 1.0f,
          pathFillType = PathFillType.NonZero,
        ) {
          moveTo(58f, 44f)
          horizontalLineTo(41f)
          curveToRelative(-1.657f, 0f, -3f, -1.343f, -3f, -3f)
          lineToRelative(0f, 0f)
          curveToRelative(0f, -1.657f, 1.343f, -3f, 3f, -3f)
          horizontalLineToRelative(17f)
          curveToRelative(1.657f, 0f, 3f, 1.343f, 3f, 3f)
          lineToRelative(0f, 0f)
          curveTo(61f, 42.657f, 59.657f, 44f, 58f, 44f)
          close()
        }
      }
      group {
        path(
          fill = SolidColor(Color(0xFFA7B3C7)),
          fillAlpha = 1.0f,
          stroke = null,
          strokeAlpha = 1.0f,
          strokeLineWidth = 1.0f,
          strokeLineCap = StrokeCap.Butt,
          strokeLineJoin = StrokeJoin.Miter,
          strokeLineMiter = 1.0f,
          pathFillType = PathFillType.NonZero,
        ) {
          moveTo(57.5f, 50f)
          horizontalLineToRelative(-16f)
          curveToRelative(-0.828f, 0f, -1.5f, -0.672f, -1.5f, -1.5f)
          reflectiveCurveToRelative(0.672f, -1.5f, 1.5f, -1.5f)
          horizontalLineToRelative(16f)
          curveToRelative(0.828f, 0f, 1.5f, 0.672f, 1.5f, 1.5f)
          reflectiveCurveTo(58.328f, 50f, 57.5f, 50f)
          close()
        }
      }
      group {
        path(
          fill = SolidColor(Color.Black),
          fillAlpha = 0.15f,
          stroke = null,
          strokeAlpha = 1.0f,
          strokeLineWidth = 1.0f,
          strokeLineCap = StrokeCap.Butt,
          strokeLineJoin = StrokeJoin.Miter,
          strokeLineMiter = 1.0f,
          pathFillType = PathFillType.NonZero,
        ) {
          moveTo(28.319f, 32.107f)
          curveTo(27.971f, 31.655f, 28.294f, 31f, 28.864f, 31f)
          horizontalLineToRelative(1.075f)
          curveToRelative(1.659f, 0f, 2.597f, -1.904f, 1.586f, -3.219f)
          lineToRelative(-5.131f, -6.674f)
          curveToRelative(-0.119f, -0.155f, -0.154f, -0.332f, -0.134f, -0.5f)
          curveToRelative(-0.236f, 0.127f, -0.471f, 0.259f, -0.69f, 0.428f)
          curveToRelative(-1.345f, 1.035f, -2.026f, 3.178f, -1.958f, 5.057f)
          curveToRelative(0.039f, 1.091f, -0.329f, 2.113f, -0.847f, 3.075f)
          curveToRelative(-0.002f, 0.003f, -0.003f, 0.005f, -0.004f, 0.008f)
          curveToRelative(-0.965f, 1.958f, -0.736f, 4.25f, 0.594f, 5.979f)
          lineTo(27.849f, 41f)
          horizontalLineToRelative(-8.634f)
          curveToRelative(-2.612f, 0f, -4.943f, 1.91f, -5.191f, 4.509f)
          curveToRelative(-0.016f, 0.166f, -0.02f, 0.329f, -0.02f, 0.491f)
          horizontalLineToRelative(18.906f)
          curveToRelative(2.489f, 0f, 3.895f, -2.855f, 2.378f, -4.829f)
          lineTo(28.319f, 32.107f)
          close()
        }
      }
      group {
        path(
          fill = SolidColor(Color(0xFFFFFFFF)),
          fillAlpha = 1.0f,
          stroke = null,
          strokeAlpha = 1.0f,
          strokeLineWidth = 1.0f,
          strokeLineCap = StrokeCap.Butt,
          strokeLineJoin = StrokeJoin.Miter,
          strokeLineMiter = 1.0f,
          pathFillType = PathFillType.NonZero,
        ) {
          moveTo(17.499f, 18f)
          curveToRelative(-0.286f, 0f, -0.575f, -0.081f, -0.831f, -0.252f)
          curveToRelative(-0.689f, -0.46f, -0.876f, -1.391f, -0.416f, -2.08f)
          lineToRelative(2f, -3f)
          curveToRelative(0.46f, -0.689f, 1.392f, -0.876f, 2.08f, -0.416f)
          curveToRelative(0.689f, 0.46f, 0.876f, 1.391f, 0.416f, 2.08f)
          lineToRelative(-2f, 3f)
          curveTo(18.459f, 17.766f, 17.983f, 18f, 17.499f, 18f)
          close()
        }
      }
    }.build()
    return _ParkBench!!
  }

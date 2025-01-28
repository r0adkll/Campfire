package app.campfire.common.compose.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.campfire.common.compose.shaders.applyNoiseEffect

private var _CampfireLogs: ImageVector? = null
private var _CampfireFireInner: ImageVector? = null
private var _CampfireFireOuter: ImageVector? = null

val CampfireIcons.CampfireLogs: ImageVector
  get() {
    if (_CampfireLogs != null) {
      return _CampfireLogs!!
    }
    _CampfireLogs = ImageVector.Builder(
      name = "Campfire",
      defaultWidth = 53.dp,
      defaultHeight = 53.dp,
      viewportWidth = 53f,
      viewportHeight = 53f,
    ).apply {
      // Log Behind
      path(
        fill = SolidColor(Color(0xFFE2B09F)),
        fillAlpha = 1.0f,
        stroke = null,
        strokeAlpha = 1.0f,
        strokeLineWidth = 1.0f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Miter,
        strokeLineMiter = 1.0f,
        pathFillType = PathFillType.NonZero,
      ) {
        moveTo(7.3680263f, 32.5996208f)
        curveToRelative(-0.9793f, 2.3966f, -0.4004f, 4.9033f, 1.3043f, 5.5999f)
        lineToRelative(29.6615868f, 12.1202202f)
        lineToRelative(3.5443192f, -8.6739006f)
        lineTo(12.2166157f, 29.5256062f)
        curveTo(10.5119f, 28.829f, 8.3432f, 30.213f, 7.368f, 32.5996f)
        close()
      }
      // Log Behind Cap
      path(
        fill = SolidColor(Color(0xFFF6CBBA)),
        fillAlpha = 1.0f,
        stroke = null,
        strokeAlpha = 1.0f,
        strokeLineWidth = 1.0f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Miter,
        strokeLineMiter = 1.0f,
        pathFillType = PathFillType.NonZero,
      ) {
        moveTo(37.0296135f, 44.719841f)
        curveToRelative(-0.9793f, 2.3966f, -0.4004f, 4.9033f, 1.3043f, 5.5999f)
        reflectiveCurveToRelative(3.8733902f, -0.687439f, 4.8526917f, -3.084053f)
        curveToRelative(0.9752f, -2.3866f, 0.3963f, -4.8933f, -1.3084f, -5.5898f)
        curveTo(40.1735f, 40.9493f, 38.0048f, 42.3333f, 37.0296f, 44.7198f)
        close()
      }
      // Front Log
      path(
        fill = SolidColor(Color(0xFFF6CBBA)),
        fillAlpha = 1.0f,
        stroke = null,
        strokeAlpha = 1.0f,
        strokeLineWidth = 1.0f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Miter,
        strokeLineMiter = 1.0f,
        pathFillType = PathFillType.NonZero,
      ) {
        moveTo(45.6319733f, 32.5996208f)
        curveToRelative(0.9793f, 2.3966f, 0.4004f, 4.9033f, -1.3043f, 5.5999f)
        lineTo(14.6661186f, 50.3197289f)
        lineToRelative(-3.5443211f, -8.6739006f)
        lineToRelative(29.6615887f, -12.1202221f)
        curveTo(42.4881f, 28.829f, 44.6568f, 30.213f, 45.632f, 32.5996f)
        close()
      }
      // Front Log Cap
      path(
        fill = SolidColor(Color(0xFFE2B09F)),
        fillAlpha = 1.0f,
        stroke = null,
        strokeAlpha = 1.0f,
        strokeLineWidth = 1.0f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Miter,
        strokeLineMiter = 1.0f,
        pathFillType = PathFillType.NonZero,
      ) {
        moveTo(15.9703865f, 44.719841f)
        curveToRelative(0.9793f, 2.3966f, 0.4004f, 4.9033f, -1.3043f, 5.5999f)
        reflectiveCurveToRelative(-3.8733892f, -0.687439f, -4.8526907f, -3.084053f)
        curveToRelative(-0.9752f, -2.3866f, -0.3963f, -4.8933f, 1.3084f, -5.5898f)
        curveTo(12.8265f, 40.9493f, 14.9952f, 42.3333f, 15.9704f, 44.7198f)
        close()
      }
    }.build()
    return _CampfireLogs!!
  }

val CampfireIcons.CampfireFireInner: ImageVector
  get() {
    if (_CampfireFireInner != null) {
      return _CampfireFireInner!!
    }
    _CampfireFireInner = ImageVector.Builder(
      name = "Campfire",
      defaultWidth = 53.dp,
      defaultHeight = 53.dp,
      viewportWidth = 53f,
      viewportHeight = 53f,
    ).apply {
      group {
        // Inner Fire
        path(
          fill = SolidColor(Color(0xFFF34624)),
          fillAlpha = 1.0f,
          stroke = null,
          strokeAlpha = 1.0f,
          strokeLineWidth = 1.0f,
          strokeLineCap = StrokeCap.Butt,
          strokeLineJoin = StrokeJoin.Miter,
          strokeLineMiter = 1.0f,
          pathFillType = PathFillType.NonZero,
        ) {
          moveTo(26.9498253f, 24.9999084f)
          curveToRelative(-0.7755f, -0.4743f, -1.7608f, -0.0158f, -1.9127f, 0.8804f)
          curveToRelative(-0.3024f, 1.7842f, -1.8528f, 3.0309f, -2.5905f, 5.0946f)
          curveToRelative(-0.8599f, 2.4056f, 0.4236f, 4.7362f, 2.8423f, 5.3638f)
          curveToRelative(2.5983f, 0.6742f, 4.9416f, -0.7221f, 5.428f, -3.2344f)
          curveTo(31.3254f, 29.9616f, 29.7492f, 26.7119f, 26.9498f, 24.9999f)
          close()
        }
      }
    }.build()
    return _CampfireFireInner!!
  }

val CampfireIcons.CampfireFireOuter: ImageVector
  get() {
    if (_CampfireFireOuter != null) {
      return _CampfireFireOuter!!
    }
    _CampfireFireOuter = ImageVector.Builder(
      name = "Campfire",
      defaultWidth = 53.dp,
      defaultHeight = 53.dp,
      viewportWidth = 53f,
      viewportHeight = 53f,
    ).apply {
      group {
        // Outer Fire
        path(
          fill = SolidColor(Color(0xFFFFC536)),
          fillAlpha = 1.0f,
          stroke = null,
          strokeAlpha = 1.0f,
          strokeLineWidth = 1.0f,
          strokeLineCap = StrokeCap.Butt,
          strokeLineJoin = StrokeJoin.Miter,
          strokeLineMiter = 1.0f,
          pathFillType = PathFillType.NonZero,
        ) {
          moveTo(18.9635773f, 14.2069721f)
          curveToRelative(0.6489f, -0.6682f, 1.7734f, -0.3001f, 1.9054f, 0.6218f)
          curveToRelative(0.0873f, 0.6099f, 0.3805f, 1.2286f, 1.0281f, 1.8692f)
          curveToRelative(2.1427f, 2.1195f, 5.2503f, 0.2538f, 5.307f, -2.2125f)
          curveToRelative(0.1209f, -5.2516f, -2.0192f, -8.8201f, 0.3375f, -11.6054f)
          curveToRelative(0.6825f, -0.8066f, 1.9962f, -0.2336f, 1.9348f, 0.8213f)
          curveToRelative(-0.2117f, 3.6347f, 1.7313f, 4.6994f, 5.2313f, 9.5736f)
          curveToRelative(7.2437f, 10.0879f, 3.1475f, 20.7067f, -4.7041f, 22.7774f)
          curveToRelative(-4.8164f, 1.2702f, -10.1188f, -0.6034f, -13.1359f, -4.6417f)
          curveTo(13.2618f, 26.5846f, 13.7082f, 19.6188f, 18.9636f, 14.207f)
          close()
        }
      }
    }.build()
    return _CampfireFireOuter!!
  }

@Composable
fun NoisyCampfireIcon(
  modifier: Modifier = Modifier,
) {
  Box(modifier) {
    Image(
      CampfireIcons.CampfireLogs,
      contentDescription = null,
      modifier = Modifier
        .fillMaxSize()
        .zIndex(0f),
    )
    Image(
      CampfireIcons.CampfireFireOuter,
      contentDescription = null,
      modifier = Modifier
        .fillMaxSize()
        .zIndex(1f)
        .applyNoiseEffect(
          frequencyX = 15f,
          frequencyY = 3f,
          speed = 0.7f,
          amplitude = 0.02f,
        ),
    )
    Image(
      CampfireIcons.CampfireFireInner,
      contentDescription = null,
      modifier = Modifier
        .fillMaxSize()
        .zIndex(2f)
        .applyNoiseEffect(
          frequencyX = 8f,
          frequencyY = 4f,
          amplitude = 0.04f,
          speed = .75f,
        ),
    )
  }
}

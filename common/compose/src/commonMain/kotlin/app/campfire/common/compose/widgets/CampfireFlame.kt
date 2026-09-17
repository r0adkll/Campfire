// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin

/**
 * The fire from the Campfire logo, drawn from the logo's own Bézier paths and deformed every frame:
 * the body squashes and stretches, a wave travels up it, its fingers see-saw in height against each
 * other, and embers rise off the top while [isBurning].
 *
 * [progress] (0..1) grows the fire up from its base, so a pull gesture visibly stokes it; the frame
 * clock only runs while the fire is showing ([progress] above zero or [isBurning]).
 */
@Composable
internal fun CampfireFlame(
  progress: () -> Float,
  isBurning: Boolean,
  modifier: Modifier = Modifier,
) {
  val currentProgress by rememberUpdatedState(progress)
  val isVisible by remember { derivedStateOf { isBurning || currentProgress() > 0f } }
  val time = remember { mutableFloatStateOf(0f) }
  LaunchedEffect(isVisible) {
    if (!isVisible) return@LaunchedEffect
    val resumedAt = time.floatValue
    val startMillis = withFrameMillis { it }
    while (true) {
      withInfiniteAnimationFrameMillis { frameMillis ->
        time.floatValue = resumedAt + (frameMillis - startMillis) / 1000f
      }
    }
  }

  val burn by animateFloatAsState(
    targetValue = if (isBurning) 1f else 0f,
    animationSpec = tween(durationMillis = 450, easing = LinearOutSlowInEasing),
  )

  Spacer(
    modifier.drawWithCache {
      val outerPoints = FloatArray(OuterFlame.size)
      val innerPoints = FloatArray(InnerFlame.size)
      val outerPath = Path()
      val innerPath = Path()
      val viewportScale = size.minDimension / FlameViewport.Size
      val viewportOffset = Offset(
        x = (size.width - FlameViewport.Size * viewportScale) / 2f - FlameViewport.Left * viewportScale,
        y = (size.height - FlameViewport.Size * viewportScale) / 2f - FlameViewport.Top * viewportScale,
      )

      onDrawBehind {
        val t = time.floatValue
        val grow = FastOutSlowInEasing.transform(currentProgress().coerceIn(0f, 1f))
        if (grow <= 0f && burn <= 0f) return@onDrawBehind

        val intensity = lerp(PullIntensity * grow, 1f, burn)
        OuterFlame.deform(t, intensity, outerPoints)
        InnerFlame.deform(t, intensity, innerPoints)
        outerPath.traceCubics(outerPoints)
        innerPath.traceCubics(innerPoints)

        translate(viewportOffset.x, viewportOffset.y) {
          scale(viewportScale, pivot = Offset.Zero) {
            scale(
              scale = lerp(MinGrowScale, 1f, maxOf(grow, burn)),
              pivot = Offset(OuterFlame.centerX, OuterFlame.baseY),
            ) {
              drawEmbers(t, burn)
              drawPath(outerPath, OuterFlameColor)
              drawPath(innerPath, InnerFlameColor)
            }
          }
        }
      }
    },
  )
}

private fun DrawScope.drawEmbers(t: Float, alpha: Float) {
  if (alpha <= 0f) return
  for (i in 0 until EmberCount) {
    val cycle = t / EmberPeriodSeconds + i.toFloat() / EmberCount
    val generation = floor(cycle).toInt()
    val life = cycle - generation
    val spread = hash01(i, generation, 0)
    val wobble = hash01(i, generation, 1)

    val rise = LinearOutSlowInEasing.transform(life)
    val x = EmberOrigin.x + (spread - 0.5f) * EmberSpread +
      sin(TAU * (life * 1.25f + wobble)) * EmberWobble
    val y = EmberOrigin.y - rise * EmberRise
    val fade = (life / EmberFadeIn).coerceAtMost(1f) * (1f - life)
    drawCircle(
      color = lerp(EmberHotColor, InnerFlameColor, wobble),
      radius = lerp(EmberStartRadius, EmberEndRadius, life),
      center = Offset(x, y),
      alpha = (fade * alpha).coerceIn(0f, 1f),
    )
  }
}

/**
 * One closed flame outline — a move-to followed by absolute cubic segments (six values each) in
 * the logo's 53×53 viewport — and how it moves. [baseY] and [height] span the flame from its root
 * to its highest tip; motion falls off toward the root so the fire stays planted.
 */
internal class FlameLayer(
  private val points: FloatArray,
  val baseY: Float,
  private val height: Float,
  val centerX: Float,
  private val stretch: Float,
  private val stretchPeriod: Float,
  private val sway: Float,
  private val swayPeriod: Float,
  private val phase: Float,
  private val tongues: List<Tongue>,
) {

  val size: Int get() = points.size

  /**
   * A finger of the outline that rises by [reach] and sinks by a fraction of it once every [period]
   * seconds ([phase] in cycles). Fingers sharing a period half a cycle apart rise and fall inversely.
   */
  class Tongue(
    val x: Float,
    val y: Float,
    val radius: Float,
    val reach: Float,
    val period: Float,
    val phase: Float,
  )

  /** Writes this outline at time [t] (seconds) into [out]; an [intensity] of 0 is the logo at rest. */
  fun deform(t: Float, intensity: Float, out: FloatArray) {
    val beat = sin(TAU * t / stretchPeriod + phase)
    val stretchY = 1f + stretch * intensity * beat
    val squeeze = 1f - stretch * SqueezeRatio * intensity * beat
    val swayAngle = TAU * t / swayPeriod + phase
    val flickerAngle = TAU * t / (swayPeriod * FlickerPeriodRatio) + phase * 1.7f

    var i = 0
    while (i < points.size) {
      val x = points[i]
      val y = points[i + 1]
      val h = ((baseY - y) / height).coerceIn(0f, 1f)

      var dx = sway * intensity * (
        h.pow(1.5f) * sin(swayAngle - h * SwayWaveLength) +
          FlickerRatio * h * h * sin(flickerAngle)
        )
      var dy = 0f
      for (j in tongues.indices) {
        val tongue = tongues[j]
        val ox = x - tongue.x
        val oy = y - tongue.y
        val falloff = exp(-(ox * ox + oy * oy) / (2f * tongue.radius * tongue.radius))
        val angle = TAU * (t / tongue.period + tongue.phase)
        dy -= tongue.reach * intensity * falloff * seesaw(angle)
        dx += tongue.reach * TongueSwayRatio * intensity * falloff * cos(angle)
      }

      out[i] = centerX + (x - centerX) * lerp(squeeze, 1f, h) + dx
      out[i + 1] = baseY - (baseY - y) * stretchY + dy
      i += 2
    }
  }
}

internal fun Path.traceCubics(points: FloatArray) {
  rewind()
  moveTo(points[0], points[1])
  var i = 2
  while (i < points.size) {
    cubicTo(points[i], points[i + 1], points[i + 2], points[i + 3], points[i + 4], points[i + 5])
    i += 6
  }
  close()
}

/**
 * A sine that reaches 1 at its crest but only -[TongueSinkRatio] at its trough, easing smoothly
 * between the two, so a finger stretches up further than it pulls back into the body.
 */
internal fun seesaw(angle: Float): Float {
  val wave = sin(angle)
  return wave * lerp(TongueSinkRatio, 1f, (wave + 1f) / 2f)
}

private fun hash01(a: Int, b: Int, salt: Int): Float {
  var h = a * 374_761_393 + b * 668_265_263 + salt * 1_442_695_041
  h = (h xor (h ushr 13)) * 1_274_126_177
  h = h xor (h ushr 16)
  return (h and 0xFFFF) / 65_535f
}

internal val OuterFlame = FlameLayer(
  points = floatArrayOf(
    18.9636f, 14.2070f,
    19.6125f, 13.5388f, 20.7370f, 13.9069f, 20.8690f, 14.8288f,
    20.9563f, 15.4387f, 21.2495f, 16.0574f, 21.8971f, 16.6980f,
    24.0398f, 18.8175f, 27.1474f, 16.9518f, 27.2041f, 14.4855f,
    27.3250f, 9.2339f, 25.1849f, 5.6654f, 27.5416f, 2.8801f,
    28.2241f, 2.0735f, 29.5378f, 2.6465f, 29.4764f, 3.7014f,
    29.2647f, 7.3361f, 31.2077f, 8.4008f, 34.7077f, 13.2750f,
    41.9514f, 23.3629f, 37.8552f, 33.9817f, 30.0036f, 36.0524f,
    25.1872f, 37.3226f, 19.8848f, 35.4490f, 16.8677f, 31.4107f,
    13.2618f, 26.5846f, 13.7082f, 19.6188f, 18.9636f, 14.2070f,
  ),
  baseY = 36.6f,
  height = 34f,
  centerX = 27f,
  stretch = 0.07f,
  stretchPeriod = 0.86f,
  sway = 1.6f,
  swayPeriod = 1.3f,
  phase = 0f,
  tongues = listOf(
    FlameLayer.Tongue(x = 28.4f, y = 4.5f, radius = 7f, reach = 4.6f, period = FingerPeriod, phase = 0f),
    FlameLayer.Tongue(x = 19.9f, y = 14.3f, radius = 4.2f, reach = 4f, period = FingerPeriod, phase = 0.5f),
  ),
)

internal val InnerFlame = FlameLayer(
  points = floatArrayOf(
    26.9498f, 24.9999f,
    26.1743f, 24.5256f, 25.1890f, 24.9841f, 25.0371f, 25.8803f,
    24.7347f, 27.6645f, 23.1843f, 28.9112f, 22.4466f, 30.9749f,
    21.5867f, 33.3805f, 22.8702f, 35.7111f, 25.2889f, 36.3387f,
    27.8872f, 37.0129f, 30.2305f, 35.6166f, 30.7169f, 33.1043f,
    31.3254f, 29.9616f, 29.7492f, 26.7119f, 26.9498f, 24.9999f,
  ),
  baseY = 36.6f,
  height = 12f,
  centerX = 26.4f,
  stretch = 0.08f,
  stretchPeriod = 0.55f,
  sway = 0.6f,
  swayPeriod = 0.95f,
  phase = PI.toFloat(),
  tongues = listOf(
    FlameLayer.Tongue(x = 26.1f, y = 24.7f, radius = 2.6f, reach = 1.4f, period = 0.48f, phase = 0.3f),
  ),
)

private object FlameViewport {
  const val Left = 5f
  const val Top = -3f
  const val Size = 42f
}

private const val TAU = (2 * PI).toFloat()

private const val PullIntensity = 0.35f
private const val MinGrowScale = 0.35f

private const val SqueezeRatio = 0.6f
private const val SwayWaveLength = 2.6f
private const val FlickerRatio = 0.35f
private const val FlickerPeriodRatio = 0.37f
private const val TongueSwayRatio = 0.25f
private const val TongueSinkRatio = 0.8f
private const val FingerPeriod = 0.66f

private const val EmberCount = 5
private const val EmberPeriodSeconds = 1.1f
private const val EmberSpread = 12f
private const val EmberWobble = 1.2f
private const val EmberRise = 18f
private const val EmberFadeIn = 0.15f
private const val EmberStartRadius = 1.6f
private const val EmberEndRadius = 0.35f
private val EmberOrigin = Offset(28f, 14f)

private val OuterFlameColor = Color(0xFFFFC536)
private val InnerFlameColor = Color(0xFFF34624)
private val EmberHotColor = Color(0xFFFFA23A)

@Preview
@Composable
private fun CampfireFlamePreview() {
  Box(
    modifier = Modifier
      .padding(12.dp)
      .size(72.dp)
      .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
  ) {
    CampfireFlame(
      progress = { 1f },
      isBurning = true,
      modifier = Modifier.padding(10.dp).size(52.dp),
    )
  }
}

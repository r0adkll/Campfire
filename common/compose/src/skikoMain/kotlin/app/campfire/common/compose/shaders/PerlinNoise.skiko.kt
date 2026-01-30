package app.campfire.common.compose.shaders

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import app.campfire.core.extensions.fluentIf
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

@Composable
actual fun Modifier.applyNoiseEffect(
  frequencyX: Float,
  frequencyY: Float,
  speed: Float,
  amplitude: Float,
  size: IntSize,
): Modifier {
  var resolution by remember { mutableStateOf(size) }

  val time by produceState(0f) {
    while (true) {
      withInfiniteAnimationFrameMillis {
        value = it / 1000f
      }
    }
  }

  return this
    .fluentIf(size == IntSize.Zero) {
      onSizeChanged { resolution = it }
    }
    .graphicsLayer {
      renderEffect = createRenderEffect(
        resolution = resolution,
        time = time,
        frequencyX = frequencyX,
        frequencyY = frequencyY,
        speed = speed,
        amplitude = amplitude,
      )
    }
}

fun createRenderEffect(
  resolution: IntSize,
  time: Float,
  frequencyX: Float,
  frequencyY: Float,
  speed: Float,
  amplitude: Float,
): RenderEffect {
  val builder = RuntimeShaderBuilder(perlinNoiseEffect).apply {
    uniform(
      "resolution",
      resolution.width.toFloat(),
      resolution.height.toFloat(),
    )
    uniform("time", time)
    uniform("frequencyX", frequencyX)
    uniform("frequencyY", frequencyY)
    uniform("speed", speed)
    uniform("amplitude", amplitude)
  }

  return ImageFilter
    .makeRuntimeShader(
      runtimeShaderBuilder = builder,
      shaderName = "contents",
      input = null,
    )
    .asComposeRenderEffect()
}

val perlinNoiseEffect: RuntimeEffect by lazy {
  RuntimeEffect.makeForShader(PerlinNoise)
}

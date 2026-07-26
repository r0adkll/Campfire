package app.campfire.common.compose.shaders

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer

@Composable
actual fun Modifier.applyNoiseEffect(
  frequencyX: Float,
  frequencyY: Float,
  speed: Float,
  amplitude: Float,
): Modifier {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return this

  val time by produceState(0f) {
    while (true) {
      withInfiniteAnimationFrameMillis {
        value = it / 1000f
      }
    }
  }

  val shader = remember { RuntimeShader(PerlinNoise) }

  return graphicsLayer {
    // A zero-sized resolution produces NaN uv offsets in the shader, rendering garbage frames
    if (size.minDimension <= 0f) {
      renderEffect = null
      return@graphicsLayer
    }

    shader.setFloatUniform("resolution", size.width, size.height)
    shader.setFloatUniform("frequencyX", frequencyX)
    shader.setFloatUniform("frequencyY", frequencyY)
    shader.setFloatUniform("speed", speed)
    shader.setFloatUniform("amplitude", amplitude)
    shader.setFloatUniform("time", time)
    renderEffect = RenderEffect
      .createRuntimeShaderEffect(shader, "contents")
      .asComposeRenderEffect()
  }
}

package app.campfire.common.compose.shaders

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import app.campfire.core.extensions.fluentIf

@Composable
actual fun Modifier.applyNoiseEffect(
  frequencyX: Float,
  frequencyY: Float,
  speed: Float,
  amplitude: Float,
  size: IntSize,
): Modifier {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return this

  val time by produceState(0f) {
    while (true) {
      withInfiniteAnimationFrameMillis {
        value = it / 1000f
      }
    }
  }

  val shader = RuntimeShader(PerlinNoise).apply {
    setFloatUniform("frequencyX", frequencyX)
    setFloatUniform("frequencyY", frequencyY)
    setFloatUniform("speed", speed)
    setFloatUniform("amplitude", amplitude)

    if (size != IntSize.Zero) {
      setFloatUniform(
        "resolution",
        size.width.toFloat(),
        size.height.toFloat(),
      )
    }
  }

  return this
    .fluentIf(size == IntSize.Zero) {
      onSizeChanged {
        shader.setFloatUniform(
          "resolution",
          it.width.toFloat(),
          it.height.toFloat(),
        )
      }
    }
    .graphicsLayer {
      shader.setFloatUniform("time", time)
      renderEffect = RenderEffect
        .createRuntimeShaderEffect(shader, "contents")
        .asComposeRenderEffect()
    }
}

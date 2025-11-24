package app.campfire.ui.theming.quantizer

import androidx.compose.ui.graphics.ImageBitmap
import com.r0adkll.swatchbuckler.compose.Swatch
import kotlinx.coroutines.flow.SharedFlow

/**
 * Queue image bitmap data to be quantized in the background
 */
interface QuantizerPipeline {

  /**
   * Observe the output of this pipeline to process incoming swatches
   */
  val outputSink: SharedFlow<QuantizedSwatch>

  /**
   * Add image bitmap data into the queue to be processed in the background
   * on a limited scheduler.
   *
   * @param key the key to store/cache the resulting quantized [Swatch] for later
   * @param image the bitmap to quantize
   */
  fun queue(
    key: String,
    image: ImageBitmap,
  )

  /**
   * Return whether or not we are currently processing an image for this [key]
   * @return true if the pipeline is processing this key, false otherwise
   */
  fun containsKey(key: String): Boolean
}

/**
 * A [Swatch] generated from an image associated with the [key] and [sourceKey]
 */
data class QuantizedSwatch(
  val key: String,
  val swatch: Swatch,
)

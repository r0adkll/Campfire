package app.campfire.ui.theming.quantizer

import androidx.compose.ui.graphics.ImageBitmap
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.logging.Cork
import co.touchlab.stately.collections.ConcurrentMutableList
import co.touchlab.stately.collections.ConcurrentMutableMap
import co.touchlab.stately.collections.ConcurrentMutableSet
import com.r0adkll.kimchi.annotations.ContributesBinding
import com.r0adkll.swatchbuckler.compose.SwatchBuilder
import kotlin.time.measureTimedValue
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class ThreadedQuantizerPipeline(
  dispatcherProvider: DispatcherProvider,
) : QuantizerPipeline {

  private val scope = CoroutineScope(
    SupervisorJob() +
      dispatcherProvider.computation.limitedParallelism(1, "quantizer") +
      CoroutineExceptionHandler { _, t ->
        ebark(throwable = t) { "Something when wrong when quantizing an image" }
      },
  )

  private val _outputSink = MutableSharedFlow<QuantizedSwatch>(
    extraBufferCapacity = 8,
    onBufferOverflow = BufferOverflow.SUSPEND,
  )
  override val outputSink: SharedFlow<QuantizedSwatch>
    get() = _outputSink.asSharedFlow()
      // If we get new subscribers emit any pending outputs immediately
      .onSubscription {
        if (outputQueue.isNotEmpty()) {
          val pending = outputQueue.toList()
          outputQueue.clear()

          pending.forEach {
            emit(it)
          }
        }
      }

  private val processingQueue = ConcurrentMutableSet<String>()
  private val processingJobs = ConcurrentMutableMap<String, Job>()
  private val outputQueue = ConcurrentMutableList<QuantizedSwatch>()

  override fun queue(key: String, image: ImageBitmap) {
    // If we are already processing this key(s), then ignore the request
    val isProcessing = processingQueue.contains(key)
    if (isProcessing) return

    // Add request to the queue
    processingQueue.add(key)

    // Start processing
    processingJobs[key] = scope.launch {
      // Quantize!!
      // TODO: Add adjustable settings to tweak max color counts
      val (swatch, duration) = measureTimedValue {
        SwatchBuilder.build(image)
      }
      vbark { "$key quantized in $duration" }

      // Emit swatch to be cached / returned
      if (swatch != null) {
        val quantizedSwatch = QuantizedSwatch(
          key = key,
          swatch = swatch,
        )

        // If there is no one to emit to, store the result until there are subscribers
        // otherwise dispatch immediately
        if (_outputSink.subscriptionCount.value == 0) {
          outputQueue.add(quantizedSwatch)
        } else {
          _outputSink.emit(quantizedSwatch)
        }
      }

      // Cleanup
      processingQueue.remove(key)
      processingJobs.remove(key)
    }
  }

  override fun containsKey(key: String): Boolean {
    return processingQueue.contains(key)
  }

  companion object : Cork {
    override val tag: String = "ThreadedQuantizerPipeline"
  }
}

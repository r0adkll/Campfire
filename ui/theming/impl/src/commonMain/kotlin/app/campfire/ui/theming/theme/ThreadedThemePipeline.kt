package app.campfire.ui.theming.theme

import app.campfire.common.compose.extensions.toHexString
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.logging.Cork
import app.campfire.ui.theming.api.SwatchSelector
import co.touchlab.stately.collections.ConcurrentMutableList
import co.touchlab.stately.collections.ConcurrentMutableMap
import co.touchlab.stately.collections.ConcurrentMutableSet
import com.r0adkll.kimchi.annotations.ContributesBinding
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpec
import com.r0adkll.swatchbuckler.compose.Schema
import com.r0adkll.swatchbuckler.compose.Swatch
import com.r0adkll.swatchbuckler.compose.ThemeBuilder
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
class ThreadedThemePipeline(
  dispatcherProvider: DispatcherProvider,
) : ThemePipeline {

  private val scope = CoroutineScope(
    SupervisorJob() +
      dispatcherProvider.computation.limitedParallelism(1, "theme-builder") +
      CoroutineExceptionHandler { _, t ->
        ebark(throwable = t) { "Something when wrong generating a theme" }
      },
  )

  private val _outputSink = MutableSharedFlow<ComputedTheme>(
    extraBufferCapacity = 8,
    onBufferOverflow = BufferOverflow.SUSPEND,
  )
  override val outputSink: SharedFlow<ComputedTheme>
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
  private val outputQueue = ConcurrentMutableList<ComputedTheme>()

  override fun queue(
    key: String,
    swatch: Swatch,
    colorSelector: SwatchSelector,
    schema: Schema,
    contrast: Double,
    spec: ColorSpec.SpecVersion,
  ) {
    // If we are already processing this key(s), then ignore the request
    val isProcessing = processingQueue.contains(key)
    if (isProcessing) {
      dbark { "Theme for $key is already being processed, ignoring." }
      return
    }

    // Add request to the queue
    processingQueue.add(key)

    // Start processing
    processingJobs[key] = scope.launch {
      // Build the theme
      val seedColor = colorSelector.selector(swatch)
      val (theme, duration) = measureTimedValue {
        ThemeBuilder()
          .seedColor(seedColor)
          .dynamicSchema(schema)
          .contrastLevel(contrast)
          .specVersion(spec)
          .build()
      }
      vbark { "$key built into theme from [${seedColor.toHexString()}] in $duration" }

      val cacheKey = ThemeCacheKeyBuilder.build(
        key = key,
        swatchSelector = colorSelector,
        schema = schema,
        contrast = contrast,
        spec = spec,
      )

      // Emit swatch to be cached / returned
      val computedTheme = ComputedTheme(
        key = key,
        cacheKey = cacheKey,
        theme = theme,
      )

      // If there is no one to emit to, store the result until there are subscribers
      // otherwise dispatch immediately
      if (_outputSink.subscriptionCount.value == 0) {
        outputQueue.add(computedTheme)
      } else {
        _outputSink.emit(computedTheme)
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
    override val tag: String = "ThreadedThemePipeline"
  }
}

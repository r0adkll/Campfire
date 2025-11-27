package app.campfire.ui.theming

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.Corked
import app.campfire.settings.api.ThemeSettings
import app.campfire.ui.theming.api.SwatchSelector
import app.campfire.ui.theming.api.ThemeManager
import app.campfire.ui.theming.cache.Cache
import app.campfire.ui.theming.cache.DiskCache
import app.campfire.ui.theming.quantizer.QuantizerPipeline
import app.campfire.ui.theming.theme.ComputedTheme
import app.campfire.ui.theming.theme.ThemeCacheKeyBuilder
import app.campfire.ui.theming.theme.ThemePipeline
import com.r0adkll.kimchi.annotations.ContributesBinding
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpec
import com.r0adkll.swatchbuckler.compose.Schema
import com.r0adkll.swatchbuckler.compose.Swatch
import com.r0adkll.swatchbuckler.compose.Theme
import kotlin.time.measureTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class CachingThemeManager(
  private val quantizerPipeline: QuantizerPipeline,
  private val themePipeline: ThemePipeline,

  private val memorySwatchCache: Cache<Swatch>,
  private val diskSwatchCache: DiskCache<Swatch>,
  private val memoryThemeCache: Cache<ComputedTheme>,
  private val diskThemeCache: DiskCache<ComputedTheme>,

  private val themeSettings: ThemeSettings,
  @ForScope(AppScope::class) private val applicationScope: CoroutineScope,
  private val dispatcherProvider: DispatcherProvider,
) : ThemeManager {

  private val themeOutputSink = MutableSharedFlow<ComputedTheme>(
    replay = 0,
    extraBufferCapacity = 8,
  )

  override fun initialize() {
    // Hydrate memory cache from disk
    applicationScope.launch(dispatcherProvider.computation) {
      val duration = measureTime {
        diskSwatchCache.selectAll().let { memorySwatchCache.putAll(it) }
        diskThemeCache.selectAll().let { memoryThemeCache.putAll(it) }
      }

      dbark { "Initializing memory cache from disk took $duration" }
    }

    // Collect events from the quantizer pipeline to cache, process, and emit
    quantizerPipeline.outputSink
      .onEach { qSwatch ->
        memorySwatchCache[qSwatch.key] = qSwatch.swatch
        diskSwatchCache[qSwatch.key] = qSwatch.swatch

        // Process into a them using defaults
        themePipeline.queue(
          key = qSwatch.key,
          swatch = qSwatch.swatch,
          colorSelector = SwatchSelector.Dominant,
          // TODO: Configure this enqueue from user adjustable settings
        )
      }
      .launchIn(applicationScope)

    themePipeline.outputSink
      .onEach { cTheme ->
        // Emit to any observers immediately for speed
        themeOutputSink.emit(cTheme)

        // Cache the result to our internal cache stages
        memoryThemeCache[cTheme.cacheKey] = cTheme
        diskThemeCache[cTheme.cacheKey] = cTheme
        // TODO: Composition cache?
      }
      .launchIn(applicationScope)
  }

  override suspend fun enqueue(key: String, image: ImageBitmap) {
    // If not enabled, then don't waste the cycles computing this here
    if (!isDynamicThemingEnabled()) return

    if (memorySwatchCache.containsKey(key)) {
      vbark { "$key already mem-cached, ignoring…" }
      return
    }

    if (diskSwatchCache.containsKey(key)) {
      vbark { "$key already disk-cached, ignoring…" }
      return
    }

    quantizerPipeline.queue(key, image)
  }

  override suspend fun enqueue(key: String, seedColor: Color) {
    // If not enabled, then don't waste the cycles computing this here
    if (!isDynamicThemingEnabled()) return

    if (memoryThemeCache.containsKey(key)) {
      vbark { "$key already mem-cached, ignoring…" }
      return
    }

    if (diskThemeCache.containsKey(key)) {
      vbark { "$key already disk-cached, ignoring…" }
      return
    }

    // TODO: Again, how to configure / pass theme builder config like scheme/contrast/etc
    themePipeline.queue(
      key = key,
      swatch = Swatch(seedColor),
      colorSelector = SwatchSelector.Dominant,
    )
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeThemeFor(
    key: String,
    colorSelector: SwatchSelector,
    schema: Schema,
    contrast: Double,
    spec: ColorSpec.SpecVersion,
  ): Flow<Theme?> {
    return themeOutputSink.asSharedFlow()
      .filter { it.key == key }
      .mapLatest { it.theme }
      .onStart<Theme?> {
        // 1) Make sure we aren't currently processing a swatch or theme for this key
        if (themePipeline.containsKey(key)) {
          vbark { "Theme pipeline already processing $key, ignoring…" }
          return@onStart
        }
        if (quantizerPipeline.containsKey(key)) {
          vbark { "Quantizer pipeline already processing $key, ignoring…" }
          return@onStart
        }

        // Build the cache key for the given them build parameters
        // and use that to attempt to fetch pre-computed hash value
        val cacheKey = ThemeCacheKeyBuilder.build(
          key = key,
          swatchSelector = colorSelector,
          schema = schema,
          contrast = contrast,
          spec = spec,
        )

        // 2) Check Memory cache
        val memoryTheme = memoryThemeCache[cacheKey]
        if (memoryTheme != null) {
          vbark { "Found theme in memory cache for $key" }
          emit(memoryTheme.theme)
          return@onStart
        }

        // 3) Check disk cache
        val diskTheme = diskThemeCache[cacheKey]
        if (diskTheme != null) {
          vbark { "Found theme in disk cache for $key" }
          memoryThemeCache[cacheKey] = diskTheme
          emit(diskTheme.theme)
          return@onStart
        }

        // 4) Check swatch cache, and re-queue into pipeline
        val swatch = memorySwatchCache[key]
        if (swatch != null) {
          vbark { "Found swatch in memory cache for $key, re-queueing into pipeline" }
          // This will be processed and emitted to this output sink we available
          themePipeline.queue(
            key = key,
            swatch = swatch,
            colorSelector = colorSelector,
            schema = schema,
            contrast = contrast,
            spec = spec,
          )
          return@onStart
        }

        // If we make it to the end be sure to emit a null so the UI can act accordingly
        emit(null)
      }
  }

  /**
   * Check if the user settings for dynamic content theming are enabled.
   */
  private fun isDynamicThemingEnabled(): Boolean {
    return themeSettings.dynamicallyThemePlayback || themeSettings.dynamicallyThemeItemDetail
  }

  companion object : Corked("CachingThemeManager")
}

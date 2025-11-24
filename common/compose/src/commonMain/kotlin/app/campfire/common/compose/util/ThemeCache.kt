package app.campfire.common.compose.util

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import app.campfire.common.compose.theme.LocalUseDarkColors
import app.campfire.core.logging.bark
import com.r0adkll.swatchbuckler.compose.Schema
import com.r0adkll.swatchbuckler.compose.Swatch
import com.r0adkll.swatchbuckler.compose.Theme
import com.r0adkll.swatchbuckler.compose.ThemeBuilder
import kotlin.time.measureTimedValue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface ThemeCache {
  val dispatcher: CoroutineDispatcher

  operator fun get(key: String): Theme?
  operator fun set(key: String, theme: Theme)
}

interface ThemeDispatcher {
  suspend fun queue(
    key: String,
    imageBitmap: ImageBitmap,
  )
}


class InMemoryThemeCache(
  override val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ThemeCache {
  private val themes: MutableMap<String, Theme> = mutableMapOf()

  override fun get(key: String): Theme? = themes[key]

  override fun set(key: String, theme: Theme) {
    themes[key] = theme
  }
}

val LocalThemeCache = compositionLocalOf<ThemeCache> {
  InMemoryThemeCache()
}

val LocalThemeDispatcher = compositionLocalOf<ThemeDispatcher> {
  error("No ThemeDispatcher in this composition")
}

/**
 * This is the magic bit that takes in the Swatches generated from cover images
 * via our Coil integration of SwatchBuckler.
 *
 * This will compute the [com.r0adkll.swatchbuckler.color.dynamiccolor.DynamicScheme] and thus
 * the final [Theme] and cache via [LocalThemeCache] configured at the root of the UI
 */
@Composable
fun rememberCachingSwatchListener(
  key: String,
  selector: (Swatch) -> Color = { it.dominant },
): (Swatch) -> Unit {
  val scope = rememberCoroutineScope()
  val cache by rememberUpdatedState(LocalThemeCache.current)
  return remember(key, cache) {
    { palette ->
      scope.launch(cache.dispatcher) {
        val seedColor = selector(palette)
        val (theme, duration) = measureTimedValue {
          ThemeBuilder()
            .seedColor(seedColor)
            .dynamicSchema(Schema.Expressive)
            .build()
        }
        bark { "[SwatchBuilder] Theme computed in $duration" }
        cache[key] = theme
      }
    }
  }
}

@Composable
fun rememberThemeDispatcherListener(
  key: String,
): (ImageBitmap) -> Unit {
  val scope = rememberCoroutineScope()
  val dispatcher by rememberUpdatedState(LocalThemeDispatcher.current)
  return remember(key, dispatcher) {
    { imageBitmap ->
      scope.launch {
        dispatcher.queue(key, imageBitmap)
      }
    }
  }
}

val Theme.colorScheme: ColorScheme
  @Composable get() = if (LocalUseDarkColors.current) darkColorScheme else lightColorScheme

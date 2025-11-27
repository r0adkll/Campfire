package app.campfire.ui.theming.api

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpec
import com.r0adkll.swatchbuckler.compose.Schema
import com.r0adkll.swatchbuckler.compose.Swatch
import com.r0adkll.swatchbuckler.compose.Theme
import kotlinx.coroutines.flow.Flow

/**
 * Manage and read the content driven themes in this application
 */
interface ThemeManager {

  /**
   * Initialize this manager hydrating colors and themes from disk cache as well as
   * starting collection on processing pipelines
   */
  fun initialize()

  /**
   * Add image bitmap data into the queue to be processed in the background
   * on a limited scheduler.
   *
   * @param key the key to store/cache the resulting quantized [Swatch] for later
   * @param image the bitmap to quantize
   */
  suspend fun enqueue(
    key: String,
    image: ImageBitmap,
  )

  /**
   * Add seed color data into the queue to be processed in the background
   * on a limited scheduler.
   *
   * @param key the key to store/cache the resulting [Theme] for later
   * @param seedColor the seed color to generate the theme from
   */
  suspend fun enqueue(
    key: String,
    seedColor: Color,
  )

  /**
   * Observe a computed theme for a given key. This will either return immediately with a
   * cached theme, or re-compute it based on the key and input parameters. If no cache exists at all then, this
   * will return null.
   *
   * This will:
   * 1. Check memory cache for a pre-computed [Theme] object for the given [key]; Return if exists.
   * 2. Check disk cache for a pre-computed [Theme] object for the given [key]; Return if exists.
   * 3. Check for [Swatch] for the given key and compute it using the passed parameters; Return if built.
   * 4. Return null if nothing else
   *
   * @param key the caching key used to store themes/swatches
   * @param colorSelector the selector that chooses the seed color for the theme
   * @param schema the [com.r0adkll.swatchbuckler.color.dynamiccolor.DynamicScheme] to use when generating a theme
   * @param contrast the contrast ratio to use when generating a theme
   * @param spec the [ColorSpec.SpecVersion] to use when generating a theme
   * @return a flow of the cached or computed theme, or null if it can't be resolved
   */
  fun observeThemeFor(
    key: String,
    colorSelector: SwatchSelector = SwatchSelector.Dominant,
    schema: Schema = Schema.Expressive,
    contrast: Double = 0.0,
    spec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2025,
  ): Flow<Theme?>
}

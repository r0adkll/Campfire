package app.campfire.ui.theming.api

import androidx.compose.ui.graphics.Color
import app.campfire.common.compose.theme.ColorPalette
import kotlinx.coroutines.flow.StateFlow

/**
 * On-device AI theme generator backed by Halogen + Gemini Nano. The feature is only
 * functional on devices that support Gemini Nano (Pixel 9 series and similar). On
 * platforms or devices without on-device GenAI support, [observeIsAvailable] emits
 * `false` permanently and [resolve] returns [HalogenResolveResult.Failed].
 */
interface HalogenThemeManager {

  /**
   * `true` when on-device AI theme generation can serve a request right now — i.e. the
   * Nano model is downloaded and ready. Drives whether the AI Theme Builder entry is
   * shown in the theme picker. Updates reactively as the model finishes downloading.
   */
  fun observeIsAvailable(): StateFlow<Boolean>

  /**
   * Generate (or fetch a cached) theme palette for [prompt].
   *
   * @param key Stable identifier used by Halogen as a cache key. Passing the same key
   *   with a different prompt will return the cached entry — pass a fresh key (e.g. a
   *   hash of the prompt) to force regeneration.
   * @param style Aesthetic preset that controls chroma levels and steers the LLM's
   *   prompt guidance (e.g. Vibrant, Muted, Pastel). Defaults to [HalogenStyle.Expressive].
   */
  suspend fun resolve(
    key: String,
    prompt: String,
    style: HalogenStyle = HalogenStyle.Expressive,
  ): HalogenResolveResult
}

/**
 * One of Halogen's eight built-in aesthetic presets. Each preset bundles chroma caps and
 * prompt guidance that steer the on-device LLM toward a particular visual style. See
 * `halogen.HalogenConfig.presets` for the source mapping.
 */
enum class HalogenStyle(val displayName: String) {
  Default("Default"),
  Vibrant("Vibrant"),
  Muted("Muted"),
  Monochrome("Monochrome"),
  Punchy("Punchy"),
  Pastel("Pastel"),
  Editorial("Editorial"),
  Expressive("Expressive"),
}

sealed interface HalogenResolveResult {
  /** Successful generation. [palette] holds the ready-to-save light + dark schemes. */
  data class Success(
    val palette: ColorPalette,
    val seedColor: Color,
  ) : HalogenResolveResult

  /** Generation could not complete — Nano unavailable, parse error, or runtime failure. */
  data class Failed(val reason: String) : HalogenResolveResult
}

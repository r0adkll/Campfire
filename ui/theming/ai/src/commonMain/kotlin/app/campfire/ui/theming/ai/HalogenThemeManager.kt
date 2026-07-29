package app.campfire.ui.theming.ai

import androidx.compose.ui.graphics.Color
import app.campfire.common.compose.theme.ColorPalette
import app.campfire.ui.theming.api.HalogenStyle
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

sealed interface HalogenResolveResult {
  /** Successful generation. [palette] holds the ready-to-save light + dark schemes. */
  data class Success(
    val palette: ColorPalette,
    val seedColor: Color,
  ) : HalogenResolveResult

  /** Generation could not complete — Nano unavailable, parse error, or runtime failure. */
  data class Failed(val reason: String) : HalogenResolveResult
}

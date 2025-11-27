package app.campfire.ui.theming.theme

import app.campfire.ui.theming.api.SwatchSelector
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpec
import com.r0adkll.swatchbuckler.compose.Schema
import com.r0adkll.swatchbuckler.compose.Swatch
import kotlinx.coroutines.flow.SharedFlow

interface ThemePipeline {

  /**
   * Observe the output of this pipeline
   */
  val outputSink: SharedFlow<ComputedTheme>

  /**
   * Queue a theme processing request
   */
  fun queue(
    key: String,
    swatch: Swatch,
    colorSelector: SwatchSelector,
    schema: Schema = Schema.Expressive,
    contrast: Double = 0.0,
    spec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2025,
  )

  /**
   * Return whether or not we are currently processing a theme for this [key]
   * @return true if the pipeline is processing this key, false otherwise
   */
  fun containsKey(key: String): Boolean
}

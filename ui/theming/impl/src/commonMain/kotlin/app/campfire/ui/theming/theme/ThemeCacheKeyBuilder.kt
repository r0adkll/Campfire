package app.campfire.ui.theming.theme

import app.campfire.ui.theming.api.SwatchSelector
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpec
import com.r0adkll.swatchbuckler.compose.Schema

object ThemeCacheKeyBuilder {

  fun build(
    key: String,
    swatchSelector: SwatchSelector,
    schema: Schema,
    contrast: Double,
    spec: ColorSpec.SpecVersion,
  ): String = buildString {
    append(key)
    append(swatchSelector.key)
    appendSchema(schema)
    append(contrast)
    appendColorSpec(spec)
  }

  private fun StringBuilder.appendSchema(schema: Schema) = append(
    when (schema) {
      Schema.Content -> "content"
      Schema.Expressive -> "expressive"
      Schema.Fidelity -> "fidelity"
      Schema.FruitSalad -> "fruit-salad"
      Schema.Monochrome -> "monochrome"
      Schema.Neutral -> "neutral"
      Schema.Rainbow -> "rainbow"
      Schema.TonalSpot -> "tonal-spot"
      Schema.Vibrant -> "vibrant"
    },
  )

  private fun StringBuilder.appendColorSpec(spec: ColorSpec.SpecVersion) = append(
    when (spec) {
      ColorSpec.SpecVersion.SPEC_2021 -> "2021"
      ColorSpec.SpecVersion.SPEC_2025 -> "2025"
    },
  )
}

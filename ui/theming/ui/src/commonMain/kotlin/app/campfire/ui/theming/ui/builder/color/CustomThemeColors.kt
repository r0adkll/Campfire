// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder.color

import androidx.compose.ui.graphics.Color
import app.campfire.common.compose.extensions.asHct
import app.campfire.common.compose.theme.ColorPalette
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpec
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpec.SpecVersion
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpecs
import com.r0adkll.swatchbuckler.color.dynamiccolor.DynamicScheme
import com.r0adkll.swatchbuckler.color.dynamiccolor.Variant
import com.r0adkll.swatchbuckler.color.palettes.TonalPalette
import com.r0adkll.swatchbuckler.compose.util.asColorScheme

/**
 * The color spec a style renders with: 2025 for the styles that spec was designed for, 2021 for
 * the classic ones.
 */
internal val Variant.specVersion: SpecVersion
  get() = when (this) {
    Variant.EXPRESSIVE,
    Variant.VIBRANT,
    Variant.TONAL_SPOT,
    Variant.NEUTRAL,
    -> SpecVersion.SPEC_2025

    Variant.MONOCHROME,
    Variant.FIDELITY,
    Variant.CONTENT,
    Variant.RAINBOW,
    Variant.FRUIT_SALAD,
    -> SpecVersion.SPEC_2021
  }

/**
 * Everything a user picks that decides a custom theme's colors.
 *
 * The seed color drives every palette that isn't overridden. A secondary, tertiary or error
 * override keeps the hue and chroma that was picked, so a vivid pick stays vivid; the scheme still
 * chooses each role's tone for light and dark mode. Neutral overrides only lend their hue, because
 * the style decides how much color backgrounds carry.
 */
internal data class CustomThemeColors(
  val seedColor: Color,
  val colorStyle: Variant,
  val colorSpec: SpecVersion = colorStyle.specVersion,
  val contrastLevel: Double = 0.0,
  val secondaryColor: Color? = null,
  val tertiaryColor: Color? = null,
  val errorColor: Color? = null,
  val neutralColor: Color? = null,
  val neutralVariantColor: Color? = null,
) {

  fun toColorPalette(): ColorPalette = ColorPalette(
    lightColorScheme = dynamicScheme(isDark = false).asColorScheme(),
    darkColorScheme = dynamicScheme(isDark = true).asColorScheme(),
  )

  fun dynamicScheme(isDark: Boolean): DynamicScheme {
    val source = seedColor.asHct()
    val spec = ColorSpecs.get(colorSpec)
    val platform = DynamicScheme.Platform.PHONE

    fun paletteFor(override: Color?, generated: ColorSpec.() -> TonalPalette): TonalPalette =
      override?.let { TonalPalette.fromHct(it.asHct()) } ?: spec.generated()

    return DynamicScheme(
      sourceColorHct = source,
      variant = colorStyle,
      isDark = isDark,
      contrastLevel = contrastLevel,
      platform = platform,
      specVersion = colorSpec,
      primaryPalette = spec.getPrimaryPalette(
        variant = colorStyle,
        sourceColorHct = source,
        isDark = isDark,
        platform = platform,
        contrastLevel = contrastLevel,
      ),
      secondaryPalette = paletteFor(secondaryColor) {
        getSecondaryPalette(
          variant = colorStyle,
          sourceColorHct = source,
          isDark = isDark,
          platform = platform,
          contrastLevel = contrastLevel,
        )
      },
      tertiaryPalette = paletteFor(tertiaryColor) {
        getTertiaryPalette(
          variant = colorStyle,
          sourceColorHct = source,
          isDark = isDark,
          platform = platform,
          contrastLevel = contrastLevel,
        )
      },
      neutralPalette = spec.getNeutralPalette(
        variant = colorStyle,
        sourceColorHct = neutralColor?.asHct() ?: source,
        isDark = isDark,
        platform = platform,
        contrastLevel = contrastLevel,
      ),
      neutralVariantPalette = spec.getNeutralVariantPalette(
        variant = colorStyle,
        sourceColorHct = neutralVariantColor?.asHct() ?: source,
        isDark = isDark,
        platform = platform,
        contrastLevel = contrastLevel,
      ),
      errorPalette = paletteFor(errorColor) {
        getErrorPalette(
          variant = colorStyle,
          sourceColorHct = source,
          isDark = isDark,
          platform = platform,
          contrastLevel = contrastLevel,
        )
      },
    )
  }
}

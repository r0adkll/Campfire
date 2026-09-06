// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.theme.desktop

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import app.campfire.common.compose.extensions.asHct
import app.campfire.common.compose.theme.ColorPalette
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpec
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpecs
import com.r0adkll.swatchbuckler.color.dynamiccolor.DynamicScheme
import com.r0adkll.swatchbuckler.color.dynamiccolor.Variant
import com.r0adkll.swatchbuckler.color.hct.Hct
import com.r0adkll.swatchbuckler.color.palettes.TonalPalette
import com.r0adkll.swatchbuckler.color.utils.MathUtils
import com.r0adkll.swatchbuckler.compose.util.asColorScheme

/**
 * HCT hue of the shared neutral ramp. Sits at the blue end so surfaces read as a cool
 * grey (light surface #F8F9FB, dark surface #111415), the conventional default for
 * desktop and web interfaces.
 */
const val DesktopNeutralHue: Double = 230.0

/**
 * HCT chroma of the neutral ramp. Material's own neutrals follow the accent hue at
 * around 6, which is what produces the visible tint on surfaces; this keeps just enough
 * colour to avoid a dead grey. The neutral-variant ramp (outlines, secondary text) uses
 * double this.
 */
const val DesktopNeutralChroma: Double = 4.0

private const val SecondaryChroma = 16.0
private const val TertiaryChroma = 24.0
private const val TertiaryHueShift = 60.0

/**
 * How the primary accent is rendered on desktop.
 *
 * Material's own tones put the dark-mode primary at tone 80, a pastel that reads as washed
 * out next to the saturated mid-tone accents desktop and web UIs use. [Punchy] raises the
 * accent's chroma to at least [minChroma] and moves the four primary roles to desktop-style
 * tones; [Material] keeps the stock tones and only applies the seed as-is.
 *
 * [variant] hands the accent palettes to one of swatchbuckler's scheme styles instead: with
 * [Variant.VIBRANT] the primary takes the maximum chroma its hue can hold and the secondary
 * and tertiary get the style's hue rotations. Null keeps the seed's own chroma for primary
 * and calm same-hue derivatives for the other two.
 *
 * `primary` doubles as text and icon colour on surfaces in Material, so the dark primary tone
 * cannot drop to the ~50 a web button would use without failing as text on a tone-6 surface;
 * the high 60s are the compromise a palette alone can reach.
 */
data class DesktopAccent(
  val minChroma: Double,
  val light: Tones,
  val dark: Tones,
  val variant: Variant? = null,
) {
  /** Tones (0-100) of the four primary roles for one colour mode. */
  data class Tones(
    val primary: Int,
    val onPrimary: Int,
    val container: Int,
    val onContainer: Int,
  )

  companion object {
    val Material = DesktopAccent(
      minChroma = 0.0,
      light = Tones(primary = 40, onPrimary = 100, container = 90, onContainer = 10),
      dark = Tones(primary = 80, onPrimary = 20, container = 30, onContainer = 90),
    )

    val Punchy = DesktopAccent(
      minChroma = 56.0,
      light = Tones(primary = 45, onPrimary = 100, container = 92, onContainer = 10),
      dark = Tones(primary = 68, onPrimary = 10, container = 35, onContainer = 95),
    )

    val Vibrant = Material.copy(variant = Variant.VIBRANT)

    /** The desktop default: maximum-chroma accent family on desktop-style tones. */
    val VibrantPunchy = Punchy.copy(variant = Variant.VIBRANT)
  }
}

/**
 * Builds the desktop flavour of a theme from its accent [seed].
 *
 * The seed's hue and chroma are kept intact as the primary accent, the secondary and
 * tertiary roles are calm derivatives of that same hue, and every neutral role
 * (backgrounds, surfaces, containers, outlines, text) comes from one shared grey ramp
 * instead of a tint of the accent. Tonal elevation also tints towards that grey rather
 * than the accent, so elevated surfaces stay neutral.
 */
fun desktopColorPalette(
  seed: Color,
  neutralHue: Double = DesktopNeutralHue,
  neutralChroma: Double = DesktopNeutralChroma,
  accent: DesktopAccent = DesktopAccent.VibrantPunchy,
): ColorPalette {
  val seedHct = seed.asHct()
  return ColorPalette(
    lightColorScheme = desktopColorScheme(seedHct, neutralHue, neutralChroma, accent, isDark = false),
    darkColorScheme = desktopColorScheme(seedHct, neutralHue, neutralChroma, accent, isDark = true),
  )
}

private fun desktopColorScheme(
  seed: Hct,
  neutralHue: Double,
  neutralChroma: Double,
  accent: DesktopAccent,
  isDark: Boolean,
): ColorScheme {
  val specVersion = ColorSpec.SpecVersion.SPEC_2021
  val platform = DynamicScheme.Platform.PHONE
  val contrastLevel = 0.0
  val spec = ColorSpecs.get(specVersion)
  val neutralPalette = TonalPalette.fromHueAndChroma(neutralHue, neutralChroma)
  val tones = if (isDark) accent.dark else accent.light
  val variant = accent.variant

  val basePrimary = if (variant == null) {
    TonalPalette.fromHct(seed)
  } else {
    spec.getPrimaryPalette(variant, seed, isDark, platform, contrastLevel)
  }
  val primaryPalette = if (basePrimary.chroma < accent.minChroma) {
    TonalPalette.fromHueAndChroma(basePrimary.hue, accent.minChroma)
  } else {
    basePrimary
  }
  val secondaryPalette = if (variant == null) {
    TonalPalette.fromHueAndChroma(seed.hue, SecondaryChroma)
  } else {
    spec.getSecondaryPalette(variant, seed, isDark, platform, contrastLevel)
  }
  val tertiaryPalette = if (variant == null) {
    TonalPalette.fromHueAndChroma(MathUtils.sanitizeDegreesDouble(seed.hue + TertiaryHueShift), TertiaryChroma)
  } else {
    spec.getTertiaryPalette(variant, seed, isDark, platform, contrastLevel)
  }

  val scheme = DynamicScheme(
    sourceColorHct = seed,
    variant = variant ?: Variant.TONAL_SPOT,
    isDark = isDark,
    contrastLevel = contrastLevel,
    platform = platform,
    specVersion = specVersion,
    primaryPalette = primaryPalette,
    secondaryPalette = secondaryPalette,
    tertiaryPalette = tertiaryPalette,
    neutralPalette = neutralPalette,
    neutralVariantPalette = TonalPalette.fromHueAndChroma(neutralHue, neutralChroma * 2),
    errorPalette = spec.getErrorPalette(
      variant = variant ?: Variant.TONAL_SPOT,
      sourceColorHct = seed,
      isDark = isDark,
      platform = platform,
      contrastLevel = contrastLevel,
    ),
  )

  return scheme.asColorScheme().copy(
    primary = Color(primaryPalette.tone(tones.primary)),
    onPrimary = Color(primaryPalette.tone(tones.onPrimary)),
    primaryContainer = Color(primaryPalette.tone(tones.container)),
    onPrimaryContainer = Color(primaryPalette.tone(tones.onContainer)),
    surfaceTint = Color(neutralPalette.tone(if (isDark) 80 else 40)),
  )
}

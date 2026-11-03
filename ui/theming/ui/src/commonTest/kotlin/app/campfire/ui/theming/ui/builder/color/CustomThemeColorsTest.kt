// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder.color

import androidx.compose.ui.graphics.Color
import app.campfire.common.compose.extensions.asHct
import app.campfire.ui.theming.api.ThemeRole
import app.campfire.ui.theming.api.colorFor
import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan
import assertk.assertions.isNotEqualTo
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpec.SpecVersion
import com.r0adkll.swatchbuckler.color.dynamiccolor.Variant
import com.r0adkll.swatchbuckler.color.hct.Hct
import com.r0adkll.swatchbuckler.color.scheme.SchemeFidelity
import com.r0adkll.swatchbuckler.color.scheme.SchemeTonalSpot
import kotlin.test.Test

class CustomThemeColorsTest {

  private val seed = Color(0xFFC0532D)

  @Test
  fun specVersion_modernStylesUse2025() {
    listOf(Variant.EXPRESSIVE, Variant.VIBRANT, Variant.TONAL_SPOT, Variant.NEUTRAL).forEach {
      assertThat(it.specVersion).isEqualTo(SpecVersion.SPEC_2025)
    }
  }

  @Test
  fun specVersion_classicStylesUse2021() {
    listOf(Variant.MONOCHROME, Variant.FIDELITY, Variant.CONTENT, Variant.RAINBOW, Variant.FRUIT_SALAD).forEach {
      assertThat(it.specVersion).isEqualTo(SpecVersion.SPEC_2021)
    }
  }

  @Test
  fun colorSpec_defaultsToTheStylesSpec() {
    assertThat(CustomThemeColors(seed, Variant.FIDELITY).colorSpec).isEqualTo(SpecVersion.SPEC_2021)
    assertThat(CustomThemeColors(seed, Variant.VIBRANT).colorSpec).isEqualTo(SpecVersion.SPEC_2025)
  }

  @Test
  fun noOverrides_matchesMaterialScheme() {
    val actual = CustomThemeColors(seed, Variant.TONAL_SPOT).dynamicScheme(isDark = false)
    val expected = SchemeTonalSpot(seed.asHct(), false, 0.0, SpecVersion.SPEC_2025)

    assertThat(actual.primary).isEqualTo(expected.primary)
    assertThat(actual.secondaryContainer).isEqualTo(expected.secondaryContainer)
    assertThat(actual.tertiary).isEqualTo(expected.tertiary)
    assertThat(actual.surface).isEqualTo(expected.surface)
    assertThat(actual.error).isEqualTo(expected.error)
  }

  @Test
  fun noOverrides_matchesMaterialSchemeForClassicStylesInDarkMode() {
    val actual = CustomThemeColors(seed, Variant.FIDELITY, contrastLevel = 0.5).dynamicScheme(isDark = true)
    val expected = SchemeFidelity(seed.asHct(), true, 0.5, SpecVersion.SPEC_2021)

    assertThat(actual.primary).isEqualTo(expected.primary)
    assertThat(actual.onSurfaceVariant).isEqualTo(expected.onSurfaceVariant)
  }

  @Test
  fun secondaryOverride_keepsPickedHueAndChroma() {
    val pick = Color(0xFF16A34A)
    val palette = CustomThemeColors(seed, Variant.TONAL_SPOT, secondaryColor = pick)
      .dynamicScheme(isDark = false)
      .secondaryPalette

    assertThat(palette.hue).isCloseTo(pick.asHct().hue, 0.5)
    assertThat(palette.chroma).isCloseTo(pick.asHct().chroma, 0.5)
  }

  @Test
  fun tertiaryOverride_keepsPickedHueAndChroma() {
    val pick = Color(0xFF00A3FF)
    val palette = CustomThemeColors(seed, Variant.NEUTRAL, tertiaryColor = pick)
      .dynamicScheme(isDark = true)
      .tertiaryPalette

    assertThat(palette.hue).isCloseTo(pick.asHct().hue, 0.5)
    assertThat(palette.chroma).isCloseTo(pick.asHct().chroma, 0.5)
  }

  @Test
  fun errorOverride_keepsPickedHueAndChroma() {
    val pick = Color(0xFFD946EF)
    val palette = CustomThemeColors(seed, Variant.EXPRESSIVE, errorColor = pick)
      .dynamicScheme(isDark = false)
      .errorPalette

    assertThat(palette.hue).isCloseTo(pick.asHct().hue, 0.5)
    assertThat(palette.chroma).isCloseTo(pick.asHct().chroma, 0.5)
  }

  @Test
  fun neutralOverride_tintsBackgroundsWithPickedHue() {
    val pick = Color(0xFF2563EB)
    val palette = CustomThemeColors(seed, Variant.TONAL_SPOT, neutralColor = pick)
      .dynamicScheme(isDark = false)
      .neutralPalette

    assertThat(palette.hue).isCloseTo(pick.asHct().hue, 1.0)
    assertThat(palette.chroma).isLessThan(pick.asHct().chroma)
  }

  @Test
  fun neutralVariantOverride_tintsWithPickedHue() {
    val pick = Color(0xFF0D9488)
    val palette = CustomThemeColors(seed, Variant.TONAL_SPOT, neutralVariantColor = pick)
      .dynamicScheme(isDark = false)
      .neutralVariantPalette

    assertThat(palette.hue).isCloseTo(pick.asHct().hue, 1.0)
  }

  @Test
  fun neutralVariantOverride_recolorsSurfaceVariantOnThe2021Spec() {
    val pick = Color(0xFF2563EB)
    val generated = CustomThemeColors(seed, Variant.FIDELITY).dynamicScheme(isDark = false)
    val overridden = CustomThemeColors(seed, Variant.FIDELITY, neutralVariantColor = pick).dynamicScheme(isDark = false)

    assertThat(overridden.surfaceVariant).isNotEqualTo(generated.surfaceVariant)
    assertThat(Hct.fromInt(overridden.surfaceVariant).hue).isCloseTo(pick.asHct().hue, 5.0)
  }

  /**
   * The 2025 spec maps surfaceVariant onto surfaceContainerHighest and reads no role from the
   * neutral variant palette, so a Neutral Variant pick has nothing to recolor there.
   */
  @Test
  fun neutralVariantOverride_leavesThe2025SpecUnchanged() {
    val generated = CustomThemeColors(seed, Variant.TONAL_SPOT).toColorPalette()
    val overridden = CustomThemeColors(seed, Variant.TONAL_SPOT, neutralVariantColor = Color(0xFF2563EB))
      .toColorPalette()

    // ColorScheme has identity equality, so compare role by role.
    ThemeRole.entries.forEach { role ->
      assertThat(overridden.lightColorScheme.colorFor(role), name = "light $role")
        .isEqualTo(generated.lightColorScheme.colorFor(role))
      assertThat(overridden.darkColorScheme.colorFor(role), name = "dark $role")
        .isEqualTo(generated.darkColorScheme.colorFor(role))
    }
  }

  @Test
  fun toColorPalette_buildsDistinctLightAndDarkSchemes() {
    val palette = CustomThemeColors(seed, Variant.VIBRANT).toColorPalette()

    assertThat(palette.lightColorScheme.surface).isNotEqualTo(palette.darkColorScheme.surface)
    assertThat(palette.lightColorScheme.primary)
      .isEqualTo(Color(CustomThemeColors(seed, Variant.VIBRANT).dynamicScheme(isDark = false).primary))
  }
}

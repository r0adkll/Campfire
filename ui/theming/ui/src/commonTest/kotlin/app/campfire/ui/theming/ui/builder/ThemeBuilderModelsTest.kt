// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder

import androidx.compose.ui.graphics.Color
import app.campfire.ui.theming.ui.builder.color.ContrastCheck
import app.campfire.ui.theming.ui.builder.color.CustomThemeColors
import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEqualTo
import assertk.assertions.isTrue
import assertk.assertions.prop
import com.r0adkll.swatchbuckler.color.dynamiccolor.Variant
import kotlin.test.Test

class ThemeBuilderModelsTest {

  private val seed = Color(0xFFC0532D)

  @Test
  fun styleOptions_listModernStylesBeforeClassicOnes() {
    val options = styleOptions(CustomThemeColors(seed, Variant.TONAL_SPOT))

    assertThat(options.map { it.style }).containsExactly(*StyleOrder.toTypedArray())
    assertThat(options.filter { it.isClassic }.map { it.style }).containsExactly(
      Variant.FIDELITY,
      Variant.CONTENT,
      Variant.MONOCHROME,
      Variant.RAINBOW,
      Variant.FRUIT_SALAD,
    )
  }

  @Test
  fun styleOptions_drawEachStyleInItsOwnColors() {
    val options = styleOptions(CustomThemeColors(seed, Variant.TONAL_SPOT))
    val vibrant = options.single { it.style == Variant.VIBRANT }
    val monochrome = options.single { it.style == Variant.MONOCHROME }

    assertThat(vibrant.light.primary).isNotEqualTo(monochrome.light.primary)
    assertThat(vibrant.light.surface).isNotEqualTo(vibrant.dark.surface)
  }

  @Test
  fun paletteRows_markOnlyPickedPalettesAsCustom() {
    val pick = Color(0xFF2563EB)
    val rows = paletteRows(CustomThemeColors(seed, Variant.TONAL_SPOT, tertiaryColor = pick))

    assertThat(rows.map { it.key }).containsExactly(*PaletteKey.entries.toTypedArray())
    assertThat(rows.single { it.key == PaletteKey.ThirdAccent }).all {
      prop(PaletteRow::isCustom).isTrue()
      prop(PaletteRow::keyColor).isEqualTo(pick)
    }
    assertThat(rows.single { it.key == PaletteKey.Supporting }.isCustom).isFalse()
    assertThat(rows.single { it.key == PaletteKey.Accent }.keyColor).isEqualTo(seed)
    rows.forEach { assertThat(it.tones).hasSize(10) }
  }

  @Test
  fun paletteRows_treatAnOlderNeutralVariantPickAsCustomBackgrounds() {
    val pick = Color(0xFF0D9488)
    val rows = paletteRows(CustomThemeColors(seed, Variant.FIDELITY, neutralVariantColor = pick))

    assertThat(rows.single { it.key == PaletteKey.Backgrounds }).all {
      prop(PaletteRow::isCustom).isTrue()
      prop(PaletteRow::keyColor).isEqualTo(pick)
    }
  }

  @Test
  fun contrastStatus_passesPerMode() {
    val status = ContrastStatus(ContrastCheck.Buttons, lightRatio = 2.1, darkRatio = 7.0)

    assertThat(status.passes(dark = false)).isFalse()
    assertThat(status.passes(dark = true)).isTrue()
    assertThat(status.passesBoth).isFalse()
  }

  @Test
  fun contrastStatuses_pairEachCheckAcrossModes() {
    val statuses = contrastStatuses(CustomThemeColors(seed, Variant.EXPRESSIVE).toColorPalette())

    assertThat(statuses.map { it.check }).containsExactly(*ContrastCheck.entries.toTypedArray())
    assertThat(statuses.single { it.check == ContrastCheck.ScreenText }.passesBoth).isTrue()
  }
}

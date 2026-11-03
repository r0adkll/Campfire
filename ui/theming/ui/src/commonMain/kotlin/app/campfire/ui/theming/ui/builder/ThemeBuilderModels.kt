// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import app.campfire.common.compose.theme.ColorPalette
import app.campfire.core.model.LibraryItem
import app.campfire.ui.theming.ui.builder.color.ContrastCheck
import app.campfire.ui.theming.ui.builder.color.CustomThemeColors
import app.campfire.ui.theming.ui.builder.color.contrastResults
import app.campfire.ui.theming.ui.builder.color.specVersion
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpec.SpecVersion
import com.r0adkll.swatchbuckler.color.dynamiccolor.DynamicScheme
import com.r0adkll.swatchbuckler.color.dynamiccolor.Variant
import com.r0adkll.swatchbuckler.color.palettes.TonalPalette

/** The palettes a user can pick a color for. [Accent] is the seed color itself. */
enum class PaletteKey {
  Accent,
  Supporting,
  ThirdAccent,
  Backgrounds,
  Errors,
}

/** A style card: the style and the colors it would give the current picks. */
@Immutable
data class StyleOption(
  val style: Variant,
  val isClassic: Boolean,
  val light: StyleSwatch,
  val dark: StyleSwatch,
)

@Immutable
data class StyleSwatch(
  val primary: Color,
  val secondaryContainer: Color,
  val tertiary: Color,
  val surface: Color,
)

/** A palette row: the color it's built from and a strip of its tones. */
@Immutable
data class PaletteRow(
  val key: PaletteKey,
  val keyColor: Color,
  val tones: List<Color>,
  val isCustom: Boolean,
)

/**
 * A book on the user's shelves and the color its details page and the player take from its
 * cover, offered as a starting color.
 */
@Immutable
data class CoverColor(
  val item: LibraryItem,
  val color: Color,
)

/** One contrast check's ratio in each mode. */
@Immutable
data class ContrastStatus(
  val check: ContrastCheck,
  val lightRatio: Double,
  val darkRatio: Double,
) {
  fun passes(dark: Boolean): Boolean = (if (dark) darkRatio else lightRatio) >= check.minimumRatio
  val passesBoth: Boolean get() = passes(dark = false) && passes(dark = true)
}

/** Modern styles first, then the classic 2021 ones. */
internal val StyleOrder = listOf(
  Variant.EXPRESSIVE,
  Variant.VIBRANT,
  Variant.TONAL_SPOT,
  Variant.NEUTRAL,
  Variant.FIDELITY,
  Variant.CONTENT,
  Variant.MONOCHROME,
  Variant.RAINBOW,
  Variant.FRUIT_SALAD,
)

private val StripTones = listOf(10, 20, 30, 40, 50, 60, 70, 80, 90, 95)

internal fun styleOptions(colors: CustomThemeColors): List<StyleOption> = StyleOrder.map { style ->
  val styled = colors.copy(colorStyle = style, colorSpec = style.specVersion)
  StyleOption(
    style = style,
    isClassic = style.specVersion == SpecVersion.SPEC_2021,
    light = styled.dynamicScheme(isDark = false).swatch(),
    dark = styled.dynamicScheme(isDark = true).swatch(),
  )
}

internal fun paletteRows(colors: CustomThemeColors): List<PaletteRow> {
  val scheme = colors.dynamicScheme(isDark = false)
  return listOf(
    PaletteRow(PaletteKey.Accent, colors.seedColor, scheme.primaryPalette.strip(), isCustom = false),
    scheme.secondaryPalette.row(PaletteKey.Supporting, colors.secondaryColor),
    scheme.tertiaryPalette.row(PaletteKey.ThirdAccent, colors.tertiaryColor),
    scheme.neutralPalette.row(PaletteKey.Backgrounds, colors.neutralColor ?: colors.neutralVariantColor),
    scheme.errorPalette.row(PaletteKey.Errors, colors.errorColor),
  )
}

internal fun contrastStatuses(palette: ColorPalette): List<ContrastStatus> =
  palette.lightColorScheme.contrastResults()
    .zip(palette.darkColorScheme.contrastResults()) { light, dark ->
      ContrastStatus(check = light.check, lightRatio = light.ratio, darkRatio = dark.ratio)
    }

private fun DynamicScheme.swatch() = StyleSwatch(
  primary = Color(primary),
  secondaryContainer = Color(secondaryContainer),
  tertiary = Color(tertiary),
  surface = Color(surfaceContainerHigh),
)

private fun TonalPalette.strip(): List<Color> = StripTones.map { Color(tone(it)) }

private fun TonalPalette.row(key: PaletteKey, override: Color?) = PaletteRow(
  key = key,
  keyColor = override ?: Color(keyColor.toInt()),
  tones = strip(),
  isCustom = override != null,
)

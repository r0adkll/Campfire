// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder.color

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import app.campfire.common.compose.extensions.asHct
import assertk.all
import assertk.assertThat
import assertk.assertions.each
import assertk.assertions.hasSize
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isTrue
import assertk.assertions.prop
import com.r0adkll.swatchbuckler.color.dynamiccolor.Variant
import kotlin.test.Test

class ContrastCheckTest {

  @Test
  fun contrastRatio_blackOnWhite_is21() {
    assertThat(contrastRatio(Color.Black, Color.White)).isCloseTo(21.0, 0.01)
  }

  @Test
  fun contrastRatio_sameColor_is1() {
    assertThat(contrastRatio(Color(0xFF7C3AED), Color(0xFF7C3AED))).isCloseTo(1.0, 0.001)
  }

  @Test
  fun contrastRatio_ignoresArgumentOrder() {
    val a = Color(0xFFFFD23F)
    val b = Color(0xFF1E3A8A)
    assertThat(contrastRatio(a, b)).isEqualTo(contrastRatio(b, a))
  }

  @Test
  fun fixForeground_alreadyPassing_returnsSameColor() {
    val foreground = Color(0xFF1E3A8A)
    assertThat(fixForeground(foreground, Color.White, TEXT_CONTRAST)).isEqualTo(foreground)
  }

  @Test
  fun fixForeground_reachesMinimumAndKeepsHue() {
    val background = Color(0xFFFFD23F)
    val foreground = Color(0xFFFFF1B8)

    val fixed = fixForeground(foreground, background, TEXT_CONTRAST)

    assertThat(contrastRatio(fixed, background)).isGreaterThanOrEqualTo(TEXT_CONTRAST)
    assertThat(fixed.asHct().hue).isCloseTo(foreground.asHct().hue, 3.0)
  }

  @Test
  fun fixForeground_nonTextMinimum_movesLessThanTextMinimum() {
    val background = Color(0xFFE9D5FF)
    val foreground = Color(0xFFD8B4FE)

    val forBar = fixForeground(foreground, background, NON_TEXT_CONTRAST)
    val forText = fixForeground(foreground, background, TEXT_CONTRAST)

    assertThat(contrastRatio(forBar, background)).isGreaterThanOrEqualTo(NON_TEXT_CONTRAST)
    assertThat(contrastRatio(forText, background)).isGreaterThanOrEqualTo(contrastRatio(forBar, background))
  }

  @Test
  fun fixForeground_unreachableRatio_fallsBackToBlackOrWhite() {
    assertThat(fixForeground(Color(0xFF7C3AED), Color.White, 30.0)).isEqualTo(Color.Black)
    assertThat(fixForeground(Color(0xFF7C3AED), Color.Black, 30.0)).isEqualTo(Color.White)
  }

  @Test
  fun colorFor_mapsEveryRoleToItsOwnSchemeColor() {
    fun distinct(role: ThemeRole) = Color(red = role.ordinal + 1, green = 0, blue = 0)
    val scheme = lightColorScheme(
      primary = distinct(ThemeRole.Primary),
      onPrimary = distinct(ThemeRole.OnPrimary),
      primaryContainer = distinct(ThemeRole.PrimaryContainer),
      onPrimaryContainer = distinct(ThemeRole.OnPrimaryContainer),
      inversePrimary = distinct(ThemeRole.InversePrimary),
      secondary = distinct(ThemeRole.Secondary),
      onSecondary = distinct(ThemeRole.OnSecondary),
      secondaryContainer = distinct(ThemeRole.SecondaryContainer),
      onSecondaryContainer = distinct(ThemeRole.OnSecondaryContainer),
      tertiary = distinct(ThemeRole.Tertiary),
      onTertiary = distinct(ThemeRole.OnTertiary),
      tertiaryContainer = distinct(ThemeRole.TertiaryContainer),
      onTertiaryContainer = distinct(ThemeRole.OnTertiaryContainer),
      error = distinct(ThemeRole.Error),
      onError = distinct(ThemeRole.OnError),
      errorContainer = distinct(ThemeRole.ErrorContainer),
      onErrorContainer = distinct(ThemeRole.OnErrorContainer),
      surface = distinct(ThemeRole.Surface),
      onSurface = distinct(ThemeRole.OnSurface),
      surfaceVariant = distinct(ThemeRole.SurfaceVariant),
      onSurfaceVariant = distinct(ThemeRole.OnSurfaceVariant),
      surfaceContainerLowest = distinct(ThemeRole.SurfaceContainerLowest),
      surfaceContainerLow = distinct(ThemeRole.SurfaceContainerLow),
      surfaceContainer = distinct(ThemeRole.SurfaceContainer),
      surfaceContainerHigh = distinct(ThemeRole.SurfaceContainerHigh),
      surfaceContainerHighest = distinct(ThemeRole.SurfaceContainerHighest),
      outline = distinct(ThemeRole.Outline),
      outlineVariant = distinct(ThemeRole.OutlineVariant),
      inverseSurface = distinct(ThemeRole.InverseSurface),
      inverseOnSurface = distinct(ThemeRole.InverseOnSurface),
      scrim = distinct(ThemeRole.Scrim),
    )

    ThemeRole.entries.forEach { role ->
      assertThat(scheme.colorFor(role), name = role.name).isEqualTo(distinct(role))
    }
  }

  @Test
  fun contrastResults_coversEveryCheck() {
    val scheme = CustomThemeColors(Color(0xFFC0532D), Variant.TONAL_SPOT).toColorPalette().lightColorScheme
    assertThat(scheme.contrastResults()).hasSize(ContrastCheck.entries.size)
  }

  @Test
  fun contrastResults_generatedSchemeKeepsTextPairsReadable() {
    val textPairs = setOf(
      ContrastCheck.ScreenText,
      ContrastCheck.PlayButtons,
      ContrastCheck.Highlights,
      ContrastCheck.SelectedItems,
      ContrastCheck.Badges,
      ContrastCheck.ErrorBanners,
      ContrastCheck.Snackbars,
    )
    val palette = CustomThemeColors(Color(0xFF2563EB), Variant.EXPRESSIVE).toColorPalette()

    listOf(palette.lightColorScheme, palette.darkColorScheme).forEach { scheme ->
      assertThat(scheme.contrastResults().filter { it.check in textPairs })
        .each { it.prop(ContrastResult::passes).isTrue() }
    }
  }

  @Test
  fun contrastResults_flagsUnreadableOverride() {
    val scheme = CustomThemeColors(Color(0xFFC0532D), Variant.TONAL_SPOT).toColorPalette().lightColorScheme
      .copy(primary = Color(0xFFFFD23F), onPrimary = Color.White)

    val playButtons = scheme.contrastResults().single { it.check == ContrastCheck.PlayButtons }

    assertThat(playButtons).all {
      prop(ContrastResult::passes).isFalse()
      prop(ContrastResult::ratio).isCloseTo(contrastRatio(Color(0xFFFFD23F), Color.White), 0.0001)
    }
  }
}

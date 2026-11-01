// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder.color

import androidx.compose.ui.graphics.Color
import app.campfire.common.compose.extensions.asHct
import app.campfire.ui.theming.api.RoleOverrides
import app.campfire.ui.theming.api.ThemeRole
import app.campfire.ui.theming.api.colorFor
import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isLessThan
import assertk.assertions.isSameInstanceAs
import com.r0adkll.swatchbuckler.color.dynamiccolor.Variant
import com.r0adkll.swatchbuckler.color.hct.Hct
import kotlin.test.Test

class FineTuneTest {

  private val generated = CustomThemeColors(Color(0xFFC0532D), Variant.EXPRESSIVE).toGeneratedPalette()

  @Test
  fun fixContrast_movesAFailingForegroundUntilItReads() {
    // Card text the same color as the card.
    val unreadable = RoleOverrides(
      light = mapOf(ThemeRole.OnSurface to generated.lightColorScheme.surfaceContainerHigh),
    )
    assertThat(ContrastCheck.CardText.ratio(unreadable, dark = false)).isLessThan(TEXT_CONTRAST)

    val fixed = unreadable.fixContrast(ContrastCheck.CardText, dark = false, generated)

    assertThat(ContrastCheck.CardText.ratio(fixed, dark = false)).isGreaterThanOrEqualTo(TEXT_CONTRAST)
    assertThat(fixed.light.keys).containsOnly(ThemeRole.OnSurface)
    assertThat(fixed.dark).isEqualTo(emptyMap())
  }

  @Test
  fun fixContrast_keepsTheForegroundsHue() {
    val reddish = Color(Hct.from(25.0, 40.0, 85.0).toInt())
    val unreadable = RoleOverrides(light = mapOf(ThemeRole.OnSurface to reddish))

    val fixed = unreadable.fixContrast(ContrastCheck.CardText, dark = false, generated)

    assertThat(fixed.light.getValue(ThemeRole.OnSurface).asHct().hue).isCloseTo(reddish.asHct().hue, 3.0)
  }

  @Test
  fun fixContrast_movesTheBackgroundWhenNoForegroundSuitsEveryPartDrawnWithIt() {
    // A dark navigation bar in light mode. Its text color also draws the search bar and the
    // drawer on pale backgrounds, so lightening the text would make those hard to read.
    val darkBar = Color(Hct.from(30.0, 10.0, 20.0).toInt())
    val tuned = RoleOverrides(light = mapOf(ThemeRole.SurfaceContainer to darkBar))
    assertThat(ContrastCheck.NavigationBar.ratio(tuned, dark = false)).isLessThan(TEXT_CONTRAST)

    val fixed = tuned.fixContrast(ContrastCheck.NavigationBar, dark = false, generated)

    assertThat(ContrastCheck.NavigationBar.ratio(fixed, dark = false)).isGreaterThanOrEqualTo(TEXT_CONTRAST)
    assertThat(fixed.light.keys).containsOnly(ThemeRole.SurfaceContainer)
    ContrastCheck.entries
      .filter { it.ratio(tuned, dark = false) >= it.minimumRatio }
      .forEach { check ->
        assertThat(check.ratio(fixed, dark = false), name = "$check").isGreaterThanOrEqualTo(check.minimumRatio)
      }
  }

  @Test
  fun fixContrast_leavesAReadablePartAlone() {
    val tuned = RoleOverrides(light = mapOf(ThemeRole.Primary to Color(0xFF8A2BE2)))

    assertThat(tuned.fixContrast(ContrastCheck.ScreenText, dark = false, generated)).isSameInstanceAs(tuned)
  }

  @Test
  fun mirror_flipsLightnessAndKeepsHueAndColorfulness() {
    val paleCard = Color(Hct.from(60.0, 20.0, 92.0).toInt())
    val tuned = RoleOverrides(light = mapOf(ThemeRole.SurfaceContainerHigh to paleCard))

    val mirrored = tuned.mirror(listOf(ThemeRole.SurfaceContainerHigh), fromDark = false, generated)

    val dark = mirrored.dark.getValue(ThemeRole.SurfaceContainerHigh).asHct()
    assertThat(dark.tone).isCloseTo(8.0, 1.0)
    assertThat(dark.hue).isCloseTo(60.0, 5.0)
    assertThat(mirrored.light).isEqualTo(tuned.light)
  }

  @Test
  fun mirror_leavesRolesThatArentTunedAlone() {
    val tuned = RoleOverrides(dark = mapOf(ThemeRole.Outline to Color(0xFF9E9E9E)))

    val mirrored = tuned.mirror(ContrastCheck.CardText.roles, fromDark = true, generated)

    assertThat(mirrored).isEqualTo(tuned)
  }

  @Test
  fun mirror_fixesThePartWhenTheFlipMakesItHardToRead() {
    // Flipping lightness doesn't keep contrast: dark text on a pale card has more of it than the
    // same tones flipped. Find a part and tone that read in light mode but not once mirrored.
    val (check, tuned) = ContrastCheck.entries.asSequence()
      .flatMap { check -> (5..95).asSequence().map { tone -> check to tone } }
      .map { (check, tone) ->
        check to RoleOverrides(light = mapOf(check.foreground to Color(Hct.from(60.0, 16.0, tone.toDouble()).toInt())))
      }
      .first { (check, tuned) ->
        val flipped = tuned.mirror(check.roles, fromDark = false, generated)
        check.ratio(tuned, dark = false) >= check.minimumRatio &&
          check.ratio(flipped, dark = true) < check.minimumRatio
      }

    val mirrored = tuned.mirror(check.roles, fromDark = false, generated, check)

    assertThat(check.ratio(mirrored, dark = true)).isGreaterThanOrEqualTo(check.minimumRatio)
    assertThat(mirrored.dark.keys).containsOnly(check.foreground)
  }

  @Test
  fun checks_listEveryPartThatDrawsWithARole() {
    assertThat(ThemeRole.SurfaceContainerHigh.checks).isEqualTo(
      listOf(
        ContrastCheck.CardText,
        ContrastCheck.SupportingText,
        ContrastCheck.DialogButtons,
        ContrastCheck.ErrorText,
      ),
    )
    assertThat(ThemeRole.Scrim.checks).isEqualTo(emptyList())
  }

  private fun ContrastCheck.ratio(overrides: RoleOverrides, dark: Boolean): Double {
    val scheme = overrides.applyTo(generated).scheme(dark)
    return contrastRatio(scheme.colorFor(foreground), scheme.colorFor(background))
  }
}

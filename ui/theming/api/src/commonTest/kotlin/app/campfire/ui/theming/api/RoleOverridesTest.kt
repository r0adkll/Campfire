// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.api

import androidx.compose.ui.graphics.Color
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import kotlin.test.Test

class RoleOverridesTest {

  private val palette = AppTheme.Fixed.Tent.colorPalette

  @Test
  fun with_setsOneModeAndNullPutsTheGeneratedColorBack() {
    val tuned = RoleOverrides.None
      .with(ThemeRole.Primary, dark = false, color = Color.Magenta)
      .with(ThemeRole.Primary, dark = true, color = Color.Cyan)

    assertThat(tuned).isEqualTo(
      RoleOverrides(light = mapOf(ThemeRole.Primary to Color.Magenta), dark = mapOf(ThemeRole.Primary to Color.Cyan)),
    )
    assertThat(tuned.with(ThemeRole.Primary, dark = false, color = null))
      .isEqualTo(RoleOverrides(dark = mapOf(ThemeRole.Primary to Color.Cyan)))
  }

  @Test
  fun without_clearsOnlyThatMode() {
    val tuned = RoleOverrides(
      light = mapOf(ThemeRole.Surface to Color.White, ThemeRole.OnSurface to Color.Black),
      dark = mapOf(ThemeRole.Surface to Color.Black),
    )

    assertThat(tuned.without(listOf(ThemeRole.Surface, ThemeRole.OnSurface), dark = false))
      .isEqualTo(RoleOverrides(dark = mapOf(ThemeRole.Surface to Color.Black)))
  }

  @Test
  fun applyTo_setsEachRoleInItsOwnMode() {
    val tuned = RoleOverrides(
      light = mapOf(ThemeRole.SecondaryContainer to Color.Magenta),
      dark = mapOf(ThemeRole.Outline to Color.Cyan),
    ).applyTo(palette)

    assertThat(tuned.lightColorScheme.secondaryContainer).isEqualTo(Color.Magenta)
    assertThat(tuned.lightColorScheme.outline).isEqualTo(palette.lightColorScheme.outline)
    assertThat(tuned.darkColorScheme.outline).isEqualTo(Color.Cyan)
    assertThat(tuned.darkColorScheme.secondaryContainer).isEqualTo(palette.darkColorScheme.secondaryContainer)
  }

  @Test
  fun applyTo_withNothingTuned_returnsThePalette() {
    assertThat(RoleOverrides.None.applyTo(palette)).isSameInstanceAs(palette)
    assertThat(RoleOverrides.None.isEmpty).isTrue()
  }

  @Test
  fun withRoleColors_movesTheRolesMaterialKeepsInStep() {
    val scheme = palette.lightColorScheme.withRoleColors(
      mapOf(ThemeRole.Surface to Color.Yellow, ThemeRole.OnSurface to Color.Blue, ThemeRole.Primary to Color.Red),
    )

    assertThat(scheme.background).isEqualTo(Color.Yellow)
    assertThat(scheme.onBackground).isEqualTo(Color.Blue)
    assertThat(scheme.surfaceTint).isEqualTo(Color.Red)
  }

  @Test
  fun withRoleColors_setsEveryRole() {
    fun distinct(role: ThemeRole) = Color(red = 0, green = role.ordinal + 1, blue = 0)

    val scheme = palette.lightColorScheme.withRoleColors(ThemeRole.entries.associateWith(::distinct))

    ThemeRole.entries.forEach { role ->
      assertThat(scheme.colorFor(role), name = role.name).isEqualTo(distinct(role))
    }
  }
}

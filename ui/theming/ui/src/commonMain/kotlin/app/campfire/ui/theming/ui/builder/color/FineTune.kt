// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder.color

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import app.campfire.common.compose.extensions.asHct
import app.campfire.common.compose.theme.ColorPalette
import app.campfire.ui.theming.api.RoleOverrides
import app.campfire.ui.theming.api.ThemeRole
import app.campfire.ui.theming.api.colorFor
import app.campfire.ui.theming.api.withRoleColors
import com.r0adkll.swatchbuckler.color.hct.Hct

/** The two roles a check compares: its background, then its foreground. */
internal val ContrastCheck.roles: List<ThemeRole> get() = listOf(background, foreground)

/** The checks that draw with this role, so fine-tuning it changes each of them. */
internal val ThemeRole.checks: List<ContrastCheck>
  get() = ContrastCheck.entries.filter { this in it.roles }

internal fun ColorPalette.scheme(dark: Boolean): ColorScheme = if (dark) darkColorScheme else lightColorScheme

/**
 * Makes [check] readable in one mode by moving its foreground's lightness as little as it takes,
 * keeping the hue and colorfulness, and keeping that as a fine-tune. Leaves the overrides alone
 * when the check already passes.
 *
 * @param generated the palette before fine-tuning, which these overrides sit on top of.
 */
internal fun RoleOverrides.fixContrast(
  check: ContrastCheck,
  dark: Boolean,
  generated: ColorPalette,
): RoleOverrides {
  val scheme = generated.scheme(dark).withRoleColors(forMode(dark))
  val foreground = scheme.colorFor(check.foreground)
  val fixed = fixForeground(foreground, scheme.colorFor(check.background), check.minimumRatio)
  return if (fixed == foreground) this else with(check.foreground, dark, fixed)
}

/**
 * Copies the fine-tuned [roles] from one mode to the other, keeping each color's hue and
 * colorfulness and flipping its lightness, so a pale light-mode background becomes a deep
 * dark-mode one. Roles that aren't fine-tuned in the first mode are left alone. When the flip
 * leaves [check] hard to read in the other mode, its contrast is fixed there too.
 */
internal fun RoleOverrides.mirror(
  roles: Collection<ThemeRole>,
  fromDark: Boolean,
  generated: ColorPalette,
  check: ContrastCheck? = null,
): RoleOverrides {
  val source = forMode(fromDark)
  val mirrored = roles.filter { it in source }.fold(this) { overrides, role ->
    overrides.with(role, dark = !fromDark, color = source.getValue(role).flipLightness())
  }
  return check?.let { mirrored.fixContrast(it, dark = !fromDark, generated) } ?: mirrored
}

private fun Color.flipLightness(): Color {
  val hct = asHct()
  return Color(Hct.from(hct.hue, hct.chroma, MAX_TONE - hct.tone).toInt())
}

private const val MAX_TONE = 100.0

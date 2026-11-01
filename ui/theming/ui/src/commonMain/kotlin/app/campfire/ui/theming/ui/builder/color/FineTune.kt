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
import kotlin.math.abs

/** The two roles a check compares: its background, then its foreground. */
internal val ContrastCheck.roles: List<ThemeRole> get() = listOf(background, foreground)

/** The checks that draw with this role, so fine-tuning it changes each of them. */
internal val ThemeRole.checks: List<ContrastCheck>
  get() = ContrastCheck.entries.filter { this in it.roles }

internal fun ColorPalette.scheme(dark: Boolean): ColorScheme = if (dark) darkColorScheme else lightColorScheme

/**
 * Makes [check] readable in one mode, keeping the hue and colorfulness of the color it moves and
 * keeping that as a fine-tune. Leaves the overrides alone when the check already passes.
 *
 * Other parts draw with the same roles, so the fix weighs the whole theme, not just [check]. Of
 * every lightness of [check]'s foreground or background that makes it readable, it picks the one
 * that leaves the fewest parts hard to read; then the foreground over the background; then the
 * smallest change. Fixing one part at a time sent the user around in circles: on a darkened
 * navigation bar, lightening its text made the search bar unreadable, and fixing that undid the
 * bar. Now that fix lightens the bar instead, and every part reads.
 *
 * When no lightness of either makes [check] readable, its foreground goes to black or white.
 *
 * @param generated the palette before fine-tuning, which these overrides sit on top of.
 */
internal fun RoleOverrides.fixContrast(
  check: ContrastCheck,
  dark: Boolean,
  generated: ColorPalette,
): RoleOverrides {
  val scheme = generated.scheme(dark).withRoleColors(forMode(dark))
  if (check.passes { scheme.colorFor(it) }) return this

  val best = listOf(check.foreground, check.background).flatMapIndexed { preference, role ->
    val hct = scheme.colorFor(role).asHct()
    (0..MAX_TONE.toInt()).mapNotNull { tone ->
      val candidate = Color(Hct.from(hct.hue, hct.chroma, tone.toDouble()).toInt())
      val colorOf = { other: ThemeRole -> if (other == role) candidate else scheme.colorFor(other) }
      if (!check.passes(colorOf)) return@mapNotNull null
      Fix(
        role = role,
        color = candidate,
        failing = ContrastCheck.entries.count { !it.passes(colorOf) },
        preference = preference,
        change = abs(tone - hct.tone),
      )
    }
  }.minWithOrNull(compareBy<Fix>({ it.failing }, { it.preference }, { it.change }))

  if (best != null) return with(best.role, dark, best.color)

  val foreground = scheme.colorFor(check.foreground)
  val fixed = fixForeground(foreground, scheme.colorFor(check.background), check.minimumRatio)
  return if (fixed == foreground) this else with(check.foreground, dark, fixed)
}

/** A color for [role] that makes a part readable, and what it costs the rest of the theme. */
private class Fix(
  val role: ThemeRole,
  val color: Color,
  /** How many parts are hard to read with it. */
  val failing: Int,
  /** 0 for the foreground, 1 for the background. */
  val preference: Int,
  /** How far its lightness moves. */
  val change: Double,
)

private inline fun ContrastCheck.passes(colorOf: (ThemeRole) -> Color): Boolean =
  contrastRatio(colorOf(foreground), colorOf(background)) >= minimumRatio

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

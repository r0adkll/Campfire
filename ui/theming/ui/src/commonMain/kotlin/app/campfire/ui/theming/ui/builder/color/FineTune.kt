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
import kotlin.math.roundToInt

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
 * Other parts draw with the same roles, so the fix never makes a part that reads now hard to
 * read. It moves the foreground's lightness as little as that allows. When no lightness does,
 * as when the background was tuned far from the other backgrounds the foreground is drawn on, it
 * moves the background instead, on the same terms. Only when neither works does it fix [check]
 * alone, moving the foreground. Fixing one part at a time used to send the user around in
 * circles: lightening the text on a darkened navigation bar made the search bar unreadable, and
 * fixing that undid the bar.
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
  if (check.passes { scheme.colorFor(it) }) return this

  val readable = ContrastCheck.entries.filter { it != check && it.passes { role -> scheme.colorFor(role) } }
  for (role in listOf(check.foreground, check.background)) {
    val fixed = nearestTone(scheme.colorFor(role)) { candidate ->
      val colorOf = { other: ThemeRole -> if (other == role) candidate else scheme.colorFor(other) }
      check.passes(colorOf) && readable.all { it.passes(colorOf) }
    }
    if (fixed != null) return with(role, dark, fixed)
  }

  val fixed = fixForeground(foreground, scheme.colorFor(check.background), check.minimumRatio)
  return if (fixed == foreground) this else with(check.foreground, dark, fixed)
}

private inline fun ContrastCheck.passes(colorOf: (ThemeRole) -> Color): Boolean =
  contrastRatio(colorOf(foreground), colorOf(background)) >= minimumRatio

/**
 * The color nearest [color] in lightness, keeping its hue and colorfulness, that [accept]s, or
 * null if none does.
 */
private inline fun nearestTone(color: Color, accept: (Color) -> Boolean): Color? {
  val hct = color.asHct()
  val start = hct.tone.roundToInt()
  for (delta in 1..MAX_TONE.toInt()) {
    for (tone in intArrayOf(start + delta, start - delta)) {
      if (tone !in 0..MAX_TONE.toInt()) continue
      val candidate = Color(Hct.from(hct.hue, hct.chroma, tone.toDouble()).toInt())
      if (accept(candidate)) return candidate
    }
  }
  return null
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

// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.api

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import app.campfire.common.compose.theme.ColorPalette

/**
 * Exact colors a user set for single roles of a custom theme, on top of the generated scheme.
 * Light and dark mode are tuned separately, since a color that reads well on one rarely reads
 * well on the other.
 */
@Immutable
data class RoleOverrides(
  val light: Map<ThemeRole, Color> = emptyMap(),
  val dark: Map<ThemeRole, Color> = emptyMap(),
) {

  val isEmpty: Boolean get() = light.isEmpty() && dark.isEmpty()

  fun forMode(dark: Boolean): Map<ThemeRole, Color> = if (dark) this.dark else light

  /** Sets [role] to [color] in one mode, or puts the generated color back when [color] is null. */
  fun with(role: ThemeRole, dark: Boolean, color: Color?): RoleOverrides {
    val updated = forMode(dark).let { if (color == null) it - role else it + (role to color) }
    return if (dark) copy(dark = updated) else copy(light = updated)
  }

  /** Puts the generated colors back for [roles] in one mode. */
  fun without(roles: Collection<ThemeRole>, dark: Boolean): RoleOverrides {
    val updated = forMode(dark) - roles.toSet()
    return if (dark) copy(dark = updated) else copy(light = updated)
  }

  /** [palette] with these colors in place of its generated ones. */
  fun applyTo(palette: ColorPalette): ColorPalette {
    if (isEmpty) return palette
    return palette.copy(
      lightColorScheme = palette.lightColorScheme.withRoleColors(light),
      darkColorScheme = palette.darkColorScheme.withRoleColors(dark),
    )
  }

  companion object {
    val None = RoleOverrides()
  }
}

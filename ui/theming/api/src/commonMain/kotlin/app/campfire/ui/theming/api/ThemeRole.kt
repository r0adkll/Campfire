// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.api

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * A color role in a Material [ColorScheme] that the theme builder can check or override. Custom
 * themes store their overrides by these names, so renaming an entry breaks saved themes.
 */
enum class ThemeRole {
  Primary,
  OnPrimary,
  PrimaryContainer,
  OnPrimaryContainer,
  InversePrimary,
  Secondary,
  OnSecondary,
  SecondaryContainer,
  OnSecondaryContainer,
  Tertiary,
  OnTertiary,
  TertiaryContainer,
  OnTertiaryContainer,
  Error,
  OnError,
  ErrorContainer,
  OnErrorContainer,
  Surface,
  OnSurface,
  SurfaceVariant,
  OnSurfaceVariant,
  SurfaceContainerLowest,
  SurfaceContainerLow,
  SurfaceContainer,
  SurfaceContainerHigh,
  SurfaceContainerHighest,
  Outline,
  OutlineVariant,
  InverseSurface,
  InverseOnSurface,
  Scrim,
}

fun ColorScheme.colorFor(role: ThemeRole): Color = when (role) {
  ThemeRole.Primary -> primary
  ThemeRole.OnPrimary -> onPrimary
  ThemeRole.PrimaryContainer -> primaryContainer
  ThemeRole.OnPrimaryContainer -> onPrimaryContainer
  ThemeRole.InversePrimary -> inversePrimary
  ThemeRole.Secondary -> secondary
  ThemeRole.OnSecondary -> onSecondary
  ThemeRole.SecondaryContainer -> secondaryContainer
  ThemeRole.OnSecondaryContainer -> onSecondaryContainer
  ThemeRole.Tertiary -> tertiary
  ThemeRole.OnTertiary -> onTertiary
  ThemeRole.TertiaryContainer -> tertiaryContainer
  ThemeRole.OnTertiaryContainer -> onTertiaryContainer
  ThemeRole.Error -> error
  ThemeRole.OnError -> onError
  ThemeRole.ErrorContainer -> errorContainer
  ThemeRole.OnErrorContainer -> onErrorContainer
  ThemeRole.Surface -> surface
  ThemeRole.OnSurface -> onSurface
  ThemeRole.SurfaceVariant -> surfaceVariant
  ThemeRole.OnSurfaceVariant -> onSurfaceVariant
  ThemeRole.SurfaceContainerLowest -> surfaceContainerLowest
  ThemeRole.SurfaceContainerLow -> surfaceContainerLow
  ThemeRole.SurfaceContainer -> surfaceContainer
  ThemeRole.SurfaceContainerHigh -> surfaceContainerHigh
  ThemeRole.SurfaceContainerHighest -> surfaceContainerHighest
  ThemeRole.Outline -> outline
  ThemeRole.OutlineVariant -> outlineVariant
  ThemeRole.InverseSurface -> inverseSurface
  ThemeRole.InverseOnSurface -> inverseOnSurface
  ThemeRole.Scrim -> scrim
}

/**
 * This scheme with [colors] in place of the generated roles. Roles Material keeps in step move
 * with them: [ColorScheme.background] follows [ThemeRole.Surface], [ColorScheme.onBackground]
 * follows [ThemeRole.OnSurface] and [ColorScheme.surfaceTint] follows [ThemeRole.Primary].
 */
fun ColorScheme.withRoleColors(colors: Map<ThemeRole, Color>): ColorScheme =
  colors.entries.fold(this) { scheme, (role, color) -> scheme.withRoleColor(role, color) }

private fun ColorScheme.withRoleColor(role: ThemeRole, color: Color): ColorScheme = when (role) {
  ThemeRole.Primary -> copy(primary = color, surfaceTint = color)
  ThemeRole.OnPrimary -> copy(onPrimary = color)
  ThemeRole.PrimaryContainer -> copy(primaryContainer = color)
  ThemeRole.OnPrimaryContainer -> copy(onPrimaryContainer = color)
  ThemeRole.InversePrimary -> copy(inversePrimary = color)
  ThemeRole.Secondary -> copy(secondary = color)
  ThemeRole.OnSecondary -> copy(onSecondary = color)
  ThemeRole.SecondaryContainer -> copy(secondaryContainer = color)
  ThemeRole.OnSecondaryContainer -> copy(onSecondaryContainer = color)
  ThemeRole.Tertiary -> copy(tertiary = color)
  ThemeRole.OnTertiary -> copy(onTertiary = color)
  ThemeRole.TertiaryContainer -> copy(tertiaryContainer = color)
  ThemeRole.OnTertiaryContainer -> copy(onTertiaryContainer = color)
  ThemeRole.Error -> copy(error = color)
  ThemeRole.OnError -> copy(onError = color)
  ThemeRole.ErrorContainer -> copy(errorContainer = color)
  ThemeRole.OnErrorContainer -> copy(onErrorContainer = color)
  ThemeRole.Surface -> copy(surface = color, background = color)
  ThemeRole.OnSurface -> copy(onSurface = color, onBackground = color)
  ThemeRole.SurfaceVariant -> copy(surfaceVariant = color)
  ThemeRole.OnSurfaceVariant -> copy(onSurfaceVariant = color)
  ThemeRole.SurfaceContainerLowest -> copy(surfaceContainerLowest = color)
  ThemeRole.SurfaceContainerLow -> copy(surfaceContainerLow = color)
  ThemeRole.SurfaceContainer -> copy(surfaceContainer = color)
  ThemeRole.SurfaceContainerHigh -> copy(surfaceContainerHigh = color)
  ThemeRole.SurfaceContainerHighest -> copy(surfaceContainerHighest = color)
  ThemeRole.Outline -> copy(outline = color)
  ThemeRole.OutlineVariant -> copy(outlineVariant = color)
  ThemeRole.InverseSurface -> copy(inverseSurface = color)
  ThemeRole.InverseOnSurface -> copy(inverseOnSurface = color)
  ThemeRole.Scrim -> copy(scrim = color)
}

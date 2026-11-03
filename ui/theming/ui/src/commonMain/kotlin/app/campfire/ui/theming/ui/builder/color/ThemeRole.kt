// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder.color

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * A color role in a Material [ColorScheme] that the theme builder can check or override.
 */
internal enum class ThemeRole {
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

internal fun ColorScheme.colorFor(role: ThemeRole): Color = when (role) {
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

package app.campfire.common.compose.theme.alt

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import app.campfire.common.compose.theme.ColorPalette

val greenSeed = Color(0xFF00FF00)

private val primaryLight = Color(0xFF026B00)
private val onPrimaryLight = Color(0xFFD3FFC2)
private val primaryContainerLight = Color(0xFF00FD00)
private val onPrimaryContainerLight = Color(0xFF015A00)
private val inversePrimaryLight = Color(0xFF00FD00)
private val secondaryLight = Color(0xFF815100)
private val onSecondaryLight = Color(0xFFFFF0E3)
private val secondaryContainerLight = Color(0xFFFFC885)
private val onSecondaryContainerLight = Color(0xFF663F00)
private val tertiaryLight = Color(0xFF00666C)
private val onTertiaryLight = Color(0xFFCEFBFF)
private val tertiaryContainerLight = Color(0xFF08EBF8)
private val onTertiaryContainerLight = Color(0xFF005257)
private val backgroundLight = Color(0xFFDFFFDF)
private val onBackgroundLight = Color(0xFF10361A)
private val surfaceLight = Color(0xFFDFFFDF)
private val onSurfaceLight = Color(0xFF10361A)
private val surfaceVariantLight = Color(0xFFB0EBB6)
private val onSurfaceVariantLight = Color(0xFF3E6444)
private val surfaceTintLight = Color(0xFF026B00)
private val inverseSurfaceLight = Color(0xFF001204)
private val inverseOnSurfaceLight = Color(0xFF7EA782)
private val errorLight = Color(0xFFB02500)
private val onErrorLight = Color(0xFFFFEFEC)
private val errorContainerLight = Color(0xFFF95630)
private val onErrorContainerLight = Color(0xFF520C00)
private val outlineLight = Color(0xFF59805E)
private val outlineVariantLight = Color(0xFF81BA89)
private val scrimLight = Color(0xFF000000)
private val surfaceBrightLight = Color(0xFFDFFFDF)
private val surfaceContainerLight = Color(0xFFC1F5C6)
private val surfaceContainerHighLight = Color(0xFFB8F0BE)
private val surfaceContainerHighestLight = Color(0xFFB0EBB6)
private val surfaceContainerLowLight = Color(0xFFCDFDD0)
private val surfaceContainerLowestLight = Color(0xFFFFFFFF)
private val surfaceDimLight = Color(0xFFA4E3AC)

private val primaryDark = Color(0xFF9FFF88)
private val onPrimaryDark = Color(0xFF026400)
private val primaryContainerDark = Color(0xFF00FD00)
private val onPrimaryContainerDark = Color(0xFF015A00)
private val inversePrimaryDark = Color(0xFF026F00)
private val secondaryDark = Color(0xFFFCA200)
private val onSecondaryDark = Color(0xFF4C2E00)
private val secondaryContainerDark = Color(0xFF855300)
private val onSecondaryContainerDark = Color(0xFFFFF6F0)
private val tertiaryDark = Color(0xFF7AF4FF)
private val onTertiaryDark = Color(0xFF005B61)
private val tertiaryContainerDark = Color(0xFF08EBF8)
private val onTertiaryContainerDark = Color(0xFF005257)
private val backgroundDark = Color(0xFF001204)
private val onBackgroundDark = Color(0xFFC5F0C8)
private val surfaceDark = Color(0xFF001204)
private val onSurfaceDark = Color(0xFFC5F0C8)
private val surfaceVariantDark = Color(0xFF002D10)
private val onSurfaceVariantDark = Color(0xFF8CB590)
private val surfaceTintDark = Color(0xFF9FFF88)
private val inverseSurfaceDark = Color(0xFFEAFFE8)
private val inverseOnSurfaceDark = Color(0xFF385D3E)
private val errorDark = Color(0xFFFF7351)
private val onErrorDark = Color(0xFF450900)
private val errorContainerDark = Color(0xFFB92902)
private val onErrorContainerDark = Color(0xFFFFD2C8)
private val outlineDark = Color(0xFF577E5D)
private val outlineVariantDark = Color(0xFF1B522B)
private val scrimDark = Color(0xFF000000)
private val surfaceBrightDark = Color(0xFF003414)
private val surfaceContainerDark = Color(0xFF001F09)
private val surfaceContainerHighDark = Color(0xFF00260D)
private val surfaceContainerHighestDark = Color(0xFF002D10)
private val surfaceContainerLowDark = Color(0xFF001806)
private val surfaceContainerLowestDark = Color(0xFF000000)
private val surfaceDimDark = Color(0xFF001204)

private val primaryFixed = Color(0xFF00FD00)
private val primaryFixedDim = Color(0xFF01ED00)
private val onPrimaryFixed = Color(0xFF014500)
private val onPrimaryFixedVariant = Color(0xFF026500)
private val secondaryFixed = Color(0xFFFFC885)
private val secondaryFixedDim = Color(0xFFFFB553)
private val onSecondaryFixed = Color(0xFF4C2E00)
private val onSecondaryFixedVariant = Color(0xFF724700)
private val tertiaryFixed = Color(0xFF08EBF8)
private val tertiaryFixedDim = Color(0xFF00DBE8)
private val onTertiaryFixed = Color(0xFF003D41)
private val onTertiaryFixedVariant = Color(0xFF005C62)

private val lightScheme = lightColorScheme(
  primary = primaryLight,
  onPrimary = onPrimaryLight,
  primaryContainer = primaryContainerLight,
  onPrimaryContainer = onPrimaryContainerLight,
  secondary = secondaryLight,
  onSecondary = onSecondaryLight,
  secondaryContainer = secondaryContainerLight,
  onSecondaryContainer = onSecondaryContainerLight,
  tertiary = tertiaryLight,
  onTertiary = onTertiaryLight,
  tertiaryContainer = tertiaryContainerLight,
  onTertiaryContainer = onTertiaryContainerLight,
  error = errorLight,
  onError = onErrorLight,
  errorContainer = errorContainerLight,
  onErrorContainer = onErrorContainerLight,
  background = backgroundLight,
  onBackground = onBackgroundLight,
  surface = surfaceLight,
  onSurface = onSurfaceLight,
  surfaceVariant = surfaceVariantLight,
  onSurfaceVariant = onSurfaceVariantLight,
  outline = outlineLight,
  outlineVariant = outlineVariantLight,
  scrim = scrimLight,
  inverseSurface = inverseSurfaceLight,
  inverseOnSurface = inverseOnSurfaceLight,
  inversePrimary = inversePrimaryLight,
  surfaceDim = surfaceDimLight,
  surfaceBright = surfaceBrightLight,
  surfaceContainerLowest = surfaceContainerLowestLight,
  surfaceContainerLow = surfaceContainerLowLight,
  surfaceContainer = surfaceContainerLight,
  surfaceContainerHigh = surfaceContainerHighLight,
  surfaceContainerHighest = surfaceContainerHighestLight,
)

private val darkScheme = darkColorScheme(
  primary = primaryDark,
  onPrimary = onPrimaryDark,
  primaryContainer = primaryContainerDark,
  onPrimaryContainer = onPrimaryContainerDark,
  secondary = secondaryDark,
  onSecondary = onSecondaryDark,
  secondaryContainer = secondaryContainerDark,
  onSecondaryContainer = onSecondaryContainerDark,
  tertiary = tertiaryDark,
  onTertiary = onTertiaryDark,
  tertiaryContainer = tertiaryContainerDark,
  onTertiaryContainer = onTertiaryContainerDark,
  error = errorDark,
  onError = onErrorDark,
  errorContainer = errorContainerDark,
  onErrorContainer = onErrorContainerDark,
  background = backgroundDark,
  onBackground = onBackgroundDark,
  surface = surfaceDark,
  onSurface = onSurfaceDark,
  surfaceVariant = surfaceVariantDark,
  onSurfaceVariant = onSurfaceVariantDark,
  outline = outlineDark,
  outlineVariant = outlineVariantDark,
  scrim = scrimDark,
  inverseSurface = inverseSurfaceDark,
  inverseOnSurface = inverseOnSurfaceDark,
  inversePrimary = inversePrimaryDark,
  surfaceDim = surfaceDimDark,
  surfaceBright = surfaceBrightDark,
  surfaceContainerLowest = surfaceContainerLowestDark,
  surfaceContainerLow = surfaceContainerLowDark,
  surfaceContainer = surfaceContainerDark,
  surfaceContainerHigh = surfaceContainerHighDark,
  surfaceContainerHighest = surfaceContainerHighestDark,
)

/**
 * @see app.campfire.core.model.Tent.Green
 */
val AltGreenColorPalette = ColorPalette(
  lightScheme,
  darkScheme,
)

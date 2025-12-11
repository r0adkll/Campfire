package app.campfire.common.compose.theme.alt

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import app.campfire.common.compose.theme.ColorPalette

private val seed = Color(0xFF0091FF)

private val primaryLight = Color(0xFF005DA6)
private val onPrimaryLight = Color(0xFFEEF3FF)
private val primaryContainerLight = Color(0xFF54A3FF)
private val onPrimaryContainerLight = Color(0xFF002345)
private val inversePrimaryLight = Color(0xFF1393FF)
private val secondaryLight = Color(0xFF8700E0)
private val onSecondaryLight = Color(0xFFFBEFFF)
private val secondaryContainerLight = Color(0xFFE6C5FF)
private val onSecondaryContainerLight = Color(0xFF6B00B3)
private val tertiaryLight = Color(0xFF803F9D)
private val onTertiaryLight = Color(0xFFFEEEFF)
private val tertiaryContainerLight = Color(0xFFE097FD)
private val onTertiaryContainerLight = Color(0xFF520C71)
private val backgroundLight = Color(0xFFF6F6FF)
private val onBackgroundLight = Color(0xFF1D2E51)
private val surfaceLight = Color(0xFFF6F6FF)
private val onSurfaceLight = Color(0xFF1D2E51)
private val surfaceVariantLight = Color(0xFFD0DCFF)
private val onSurfaceVariantLight = Color(0xFF4B5B81)
private val surfaceTintLight = Color(0xFF005DA6)
private val inverseSurfaceLight = Color(0xFF000D29)
private val inverseOnSurfaceLight = Color(0xFF8C9CC6)
private val errorLight = Color(0xFFB31B25)
private val onErrorLight = Color(0xFFFFEFEE)
private val errorContainerLight = Color(0xFFFB5151)
private val onErrorContainerLight = Color(0xFF570008)
private val outlineLight = Color(0xFF66769E)
private val outlineVariantLight = Color(0xFF95ACE6)
private val scrimLight = Color(0xFF000000)
private val surfaceBrightLight = Color(0xFFF6F6FF)
private val surfaceContainerLight = Color(0xFFE1E8FF)
private val surfaceContainerHighLight = Color(0xFFD9E2FF)
private val surfaceContainerHighestLight = Color(0xFFD0DCFF)
private val surfaceContainerLowLight = Color(0xFFEDF0FF)
private val surfaceContainerLowestLight = Color(0xFFFFFFFF)
private val surfaceDimLight = Color(0xFFC4D4FF)

private val primaryDark = Color(0xFF74B1FF)
private val onPrimaryDark = Color(0xFF002F59)
private val primaryContainerDark = Color(0xFF54A3FF)
private val onPrimaryContainerDark = Color(0xFF002345)
private val inversePrimaryDark = Color(0xFF0060AC)
private val secondaryDark = Color(0xFFC280FF)
private val onSecondaryDark = Color(0xFF33005A)
private val secondaryContainerDark = Color(0xFF8B00E7)
private val onSecondaryContainerDark = Color(0xFFFEF5FF)
private val tertiaryDark = Color(0xFFE7AAFF)
private val onTertiaryDark = Color(0xFF5D1B7B)
private val tertiaryContainerDark = Color(0xFFE097FD)
private val onTertiaryContainerDark = Color(0xFF520C71)
private val backgroundDark = Color(0xFF000D29)
private val onBackgroundDark = Color(0xFFDDE5FF)
private val surfaceDark = Color(0xFF000D29)
private val onSurfaceDark = Color(0xFFDDE5FF)
private val surfaceVariantDark = Color(0xFF032455)
private val onSurfaceVariantDark = Color(0xFF9AABD5)
private val surfaceTintDark = Color(0xFF74B1FF)
private val inverseSurfaceDark = Color(0xFFFAF9FF)
private val inverseOnSurfaceDark = Color(0xFF44557A)
private val errorDark = Color(0xFFFF716C)
private val onErrorDark = Color(0xFF490006)
private val errorContainerDark = Color(0xFF9F0519)
private val onErrorContainerDark = Color(0xFFFFA8A3)
private val outlineDark = Color(0xFF65759C)
private val outlineVariantDark = Color(0xFF2E4779)
private val scrimDark = Color(0xFF000000)
private val surfaceBrightDark = Color(0xFF082A5F)
private val surfaceContainerDark = Color(0xFF001840)
private val surfaceContainerHighDark = Color(0xFF001D4B)
private val surfaceContainerHighestDark = Color(0xFF032455)
private val surfaceContainerLowDark = Color(0xFF001233)
private val surfaceContainerLowestDark = Color(0xFF000000)
private val surfaceDimDark = Color(0xFF000D29)

private val primaryFixed = Color(0xFF54A3FF)
private val primaryFixedDim = Color(0xFF2695FF)
private val onPrimaryFixed = Color(0xFF000000)
private val onPrimaryFixedVariant = Color(0xFF002D55)
private val secondaryFixed = Color(0xFFE6C5FF)
private val secondaryFixedDim = Color(0xFFDCB3FF)
private val onSecondaryFixed = Color(0xFF500088)
private val onSecondaryFixedVariant = Color(0xFF7800C8)
private val tertiaryFixed = Color(0xFFE097FD)
private val tertiaryFixedDim = Color(0xFFD18AEF)
private val onTertiaryFixed = Color(0xFF310046)
private val onTertiaryFixedVariant = Color(0xFF5C1A7A)

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
 * @see app.campfire.core.model.Tent.Blue
 */
val AltBlueColorPalette = ColorPalette(
  lightScheme,
  darkScheme,
)

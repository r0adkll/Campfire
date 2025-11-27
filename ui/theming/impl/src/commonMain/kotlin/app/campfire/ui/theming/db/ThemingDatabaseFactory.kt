package app.campfire.ui.theming.db

import app.campfire.themes.CampfireThemeDatabase
import app.campfire.themes.ColorScheme
import app.campfire.themes.Swatch
import app.cash.sqldelight.db.SqlDriver
import me.tatarka.inject.annotations.Inject

@Inject
class ThemingDatabaseFactory(
  @ThemingDb private val driver: SqlDriver,
) {

  fun create() = CampfireThemeDatabase(
    driver = driver,
    colorSchemeAdapter = ColorScheme.Adapter(
      primaryColorAdapter = ColorColumnAdapter,
      onPrimaryAdapter = ColorColumnAdapter,
      primaryContainerAdapter = ColorColumnAdapter,
      onPrimaryContainerAdapter = ColorColumnAdapter,
      inversePrimaryAdapter = ColorColumnAdapter,
      secondaryAdapter = ColorColumnAdapter,
      onSecondaryAdapter = ColorColumnAdapter,
      secondaryContainerAdapter = ColorColumnAdapter,
      onSecondaryContainerAdapter = ColorColumnAdapter,
      tertiaryAdapter = ColorColumnAdapter,
      onTertiaryAdapter = ColorColumnAdapter,
      tertiaryContainerAdapter = ColorColumnAdapter,
      onTertiaryContainerAdapter = ColorColumnAdapter,
      backgroundAdapter = ColorColumnAdapter,
      onBackgroundAdapter = ColorColumnAdapter,
      surfaceAdapter = ColorColumnAdapter,
      onSurfaceAdapter = ColorColumnAdapter,
      surfaceVariantAdapter = ColorColumnAdapter,
      onSurfaceVariantAdapter = ColorColumnAdapter,
      surfaceTintAdapter = ColorColumnAdapter,
      inverseSurfaceAdapter = ColorColumnAdapter,
      inverseOnSurfaceAdapter = ColorColumnAdapter,
      errorAdapter = ColorColumnAdapter,
      onErrorAdapter = ColorColumnAdapter,
      errorContainerAdapter = ColorColumnAdapter,
      onErrorContainerAdapter = ColorColumnAdapter,
      outlineAdapter = ColorColumnAdapter,
      outlineVariantAdapter = ColorColumnAdapter,
      scrimAdapter = ColorColumnAdapter,
      surfaceBrightAdapter = ColorColumnAdapter,
      surfaceDimAdapter = ColorColumnAdapter,
      surfaceContainerAdapter = ColorColumnAdapter,
      surfaceContainerHighAdapter = ColorColumnAdapter,
      surfaceContainerHighestAdapter = ColorColumnAdapter,
      surfaceContainerLowAdapter = ColorColumnAdapter,
      surfaceContainerLowestAdapter = ColorColumnAdapter,
      primaryFixedAdapter = ColorColumnAdapter,
      primaryFixedDimAdapter = ColorColumnAdapter,
      onPrimaryFixedAdapter = ColorColumnAdapter,
      onPrimaryFixedVariantAdapter = ColorColumnAdapter,
      secondaryFixedAdapter = ColorColumnAdapter,
      secondaryFixedDimAdapter = ColorColumnAdapter,
      onSecondaryFixedAdapter = ColorColumnAdapter,
      onSecondaryFixedVariantAdapter = ColorColumnAdapter,
      tertiaryFixedAdapter = ColorColumnAdapter,
      tertiaryFixedDimAdapter = ColorColumnAdapter,
      onTertiaryFixedAdapter = ColorColumnAdapter,
      onTertiaryFixedVariantAdapter = ColorColumnAdapter,
    ),
    swatchAdapter = Swatch.Adapter(
      dominantAdapter = ColorColumnAdapter,
      vibrantAdapter = ColorListColumnAdapter,
    ),
  )
}

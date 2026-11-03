// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming

import androidx.compose.ui.graphics.Color
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.logging.bark
import app.campfire.settings.api.ThemeKey
import app.campfire.settings.api.ThemeSettings
import app.campfire.themes.CampfireThemeDatabase
import app.campfire.themes.CustomAppTheme
import app.campfire.themes.Theme
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.AppThemeRepository
import app.campfire.ui.theming.db.mapping.asDbModel
import app.campfire.ui.theming.db.mapping.asDbModels
import app.campfire.ui.theming.db.mapping.asDomainModel
import app.campfire.ui.theming.db.mapping.asRoleOverrides
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpec
import com.r0adkll.swatchbuckler.color.dynamiccolor.Variant
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.measureTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DefaultAppThemeRepository(
  private val themeSettings: ThemeSettings,
  private val themingDb: CampfireThemeDatabase,
  private val dispatcherProvider: DispatcherProvider,
) : AppThemeRepository {

  private val themeCache = mutableMapOf<String, AppTheme.Fixed>()

  private val currentAppTheme = MutableStateFlow<AppTheme>(AppTheme.Fixed.Tent)

  suspend fun initialize() = measureTime {
    val theme = when (val key = themeSettings.themeId) {
      ThemeKey.Tent -> AppTheme.Fixed.Tent
      ThemeKey.WaterBottle -> AppTheme.Fixed.WaterBottle
      ThemeKey.Forest -> AppTheme.Fixed.Forest
      ThemeKey.LifeFloat -> AppTheme.Fixed.LifeFloat
      ThemeKey.Mountain -> AppTheme.Fixed.Mountain
      ThemeKey.Rucksack -> AppTheme.Fixed.Rucksack
      ThemeKey.Dynamic -> AppTheme.Dynamic
      is ThemeKey.Custom -> {
        loadCustomTheme(key.id)
          ?.also { themeCache[key.id] = it }
          ?: AppTheme.Fixed.Tent
      }
    }

    currentAppTheme.value = theme
  }.also { duration ->
    bark { "ThemeRepository initialized in $duration" }
  }

  override fun observeCurrentAppTheme(): StateFlow<AppTheme> {
    return currentAppTheme
  }

  override fun observeCustomThemes(): Flow<List<AppTheme.Fixed>> {
    val themes = themingDb.customAppThemeQueries
      .selectAll()
      .asFlow()
      .mapToList(dispatcherProvider.databaseRead)
    val overrides = themingDb.customAppThemeRoleOverrideQueries
      .selectAll()
      .asFlow()
      .mapToList(dispatcherProvider.databaseRead)

    return combine(themes, overrides) { themeEntities, overrideEntities ->
      val overridesByTheme = overrideEntities.groupBy { it.themeId }
      themeEntities.map { dbEntity ->
        dbEntity.asDomainModel(overridesByTheme[dbEntity.id].orEmpty().asRoleOverrides())
      }
    }
      .onEach { themes ->
        // Update the local cache
        themeCache.putAll(
          themes.associateBy { it.id },
        )
      }
  }

  override fun setCurrentTheme(theme: AppTheme) {
    currentAppTheme.value = theme
    themeSettings.themeId = when (theme) {
      AppTheme.Dynamic -> ThemeKey.Dynamic
      AppTheme.Fixed.Forest -> ThemeKey.Forest
      AppTheme.Fixed.LifeFloat -> ThemeKey.LifeFloat
      AppTheme.Fixed.Mountain -> ThemeKey.Mountain
      AppTheme.Fixed.Rucksack -> ThemeKey.Rucksack
      AppTheme.Fixed.Tent -> ThemeKey.Tent
      AppTheme.Fixed.WaterBottle -> ThemeKey.WaterBottle
      is AppTheme.Fixed.Custom -> ThemeKey.Custom(theme.id)
      is AppTheme.Fixed.Ai -> ThemeKey.Custom(theme.id)
    }
  }

  override suspend fun getCustomTheme(id: String): Result<AppTheme.Fixed> {
    val cached = themeCache[id]
    return if (cached != null) {
      Result.success(cached)
    } else {
      val diskCached = loadCustomTheme(id)

      if (diskCached != null) {
        themeCache[id] = diskCached
        Result.success(diskCached)
      } else {
        Result.failure(IllegalArgumentException("Unable to find custom theme for the id '$id'"))
      }
    }
  }

  private suspend fun loadCustomTheme(id: String): AppTheme.Fixed? {
    val entity = themingDb.customAppThemeQueries
      .selectById(id)
      .awaitAsOneOrNull()
      ?: return null
    val overrides = themingDb.customAppThemeRoleOverrideQueries
      .selectForTheme(id)
      .awaitAsList()
    return entity.asDomainModel(overrides.asRoleOverrides())
  }

  override suspend fun saveCustomTheme(theme: AppTheme.Fixed) {
    when (theme) {
      is AppTheme.Fixed.Ai -> saveAiTheme(theme)
      is AppTheme.Fixed.Custom -> saveCustomTheme(theme)
      else -> Unit
    }
  }

  private suspend fun saveAiTheme(theme: AppTheme.Fixed.Ai) {
    // Cache in memory
    themeCache[theme.id] = theme

    val isCurrentTheme = (themeSettings.themeId as? ThemeKey.Custom)?.id == theme.id
    if (isCurrentTheme) {
      setCurrentTheme(theme)
    }

    // Persist to disk
    withContext(dispatcherProvider.databaseWrite) {
      themingDb.customAppThemeQueries.transaction {
        // 1) Insert custom app theme
        themingDb.customAppThemeQueries.insert(
          CustomAppTheme(
            id = theme.id,
            name = theme.name,
            icon = theme.icon,
            seedColor = Color.Transparent,
            secondaryColorOverride = null,
            tertiaryColorOverride = null,
            errorColorOverride = null,
            neutralColorOverride = null,
            neutralVariantColorOverride = null,
            colorSpec = ColorSpec.SpecVersion.SPEC_2021,
            colorStyle = Variant.EXPRESSIVE,
            contrastLevel = -1.0,
            isAi = true,
            prompt = theme.prompt,
            style = theme.style,
          ),
        )

        // 2) Insert theme
        themingDb.themeQueries.insertTheme(
          Theme(
            cacheKey = theme.id,
            key = "custom-${theme.id}",
          ),
        )

        // 3) Insert color palette
        themingDb.themeQueries.insertColorScheme(
          theme.colorPalette.lightColorScheme.asDbModel(theme.id, isDark = false),
        )
        themingDb.themeQueries.insertColorScheme(
          theme.colorPalette.darkColorScheme.asDbModel(theme.id, isDark = true),
        )
      }
    }
  }

  private suspend fun saveCustomTheme(theme: AppTheme.Fixed.Custom) {
    // Cache in memory
    themeCache[theme.id] = theme

    val isCurrentTheme = (themeSettings.themeId as? ThemeKey.Custom)?.id == theme.id
    if (isCurrentTheme) {
      setCurrentTheme(theme)
    }

    // Persist to disk
    withContext(dispatcherProvider.databaseWrite) {
      themingDb.customAppThemeQueries.transaction {
        // 1) Insert custom app theme
        themingDb.customAppThemeQueries.insert(
          CustomAppTheme(
            id = theme.id,
            name = theme.name,
            icon = theme.icon,
            seedColor = theme.seedColor,
            secondaryColorOverride = theme.secondaryColorOverride,
            tertiaryColorOverride = theme.tertiaryColorOverride,
            errorColorOverride = theme.errorColorOverride,
            neutralColorOverride = theme.neutralColorOverride,
            neutralVariantColorOverride = theme.neutralVariantColorOverride,
            colorSpec = theme.colorSpec,
            colorStyle = theme.colorStyle,
            contrastLevel = theme.contrastLevel.toDouble(),
            isAi = false,
            prompt = null,
            style = null,
          ),
        )

        // 2) Replace its fine-tuned colors. Replacing the theme row above may already have
        //    cleared them through the foreign key, so this never relies on either order.
        themingDb.customAppThemeRoleOverrideQueries.deleteForTheme(theme.id)
        theme.roleOverrides.asDbModels(theme.id).forEach { override ->
          themingDb.customAppThemeRoleOverrideQueries.insert(override)
        }

        // 3) Insert theme
        themingDb.themeQueries.insertTheme(
          Theme(
            cacheKey = theme.id,
            key = "custom-${theme.id}",
          ),
        )

        // 4) Insert color palette
        themingDb.themeQueries.insertColorScheme(
          theme.colorPalette.lightColorScheme.asDbModel(theme.id, isDark = false),
        )
        themingDb.themeQueries.insertColorScheme(
          theme.colorPalette.darkColorScheme.asDbModel(theme.id, isDark = true),
        )
      }
    }
  }

  override suspend fun deleteCustomTheme(id: String) {
    themeCache.remove(id)
    themingDb.customAppThemeRoleOverrideQueries.deleteForTheme(id)
    themingDb.customAppThemeQueries.delete(id)
    themingDb.themeQueries.deleteTheme(id)

    val isCurrentTheme = (themeSettings.themeId as? ThemeKey.Custom)?.id == id
    if (isCurrentTheme) {
      setCurrentTheme(AppTheme.Fixed.Tent)
    }
  }
}

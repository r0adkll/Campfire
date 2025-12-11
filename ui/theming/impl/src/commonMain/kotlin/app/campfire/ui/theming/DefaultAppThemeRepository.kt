package app.campfire.ui.theming

import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.logging.bark
import app.campfire.settings.api.CampfireSettings
import app.campfire.settings.api.ThemeKey
import app.campfire.themes.CampfireThemeDatabase
import app.campfire.themes.CustomAppTheme
import app.campfire.themes.Theme
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.AppThemeRepository
import app.campfire.ui.theming.db.mapping.asDbModel
import app.campfire.ui.theming.db.mapping.asDomainModel
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.time.measureTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DefaultAppThemeRepository(
  private val campfireSettings: CampfireSettings,
  private val themingDb: CampfireThemeDatabase,
  private val dispatcherProvider: DispatcherProvider,
) : AppThemeRepository {

  private val themeCache = mutableMapOf<String, AppTheme.Fixed.Custom>()

  private val currentAppTheme = MutableStateFlow<AppTheme>(AppTheme.Fixed.Tent)

  suspend fun initialize() = measureTime {
    val theme = when (val key = campfireSettings.themeId) {
      ThemeKey.Tent -> AppTheme.Fixed.Tent
      ThemeKey.WaterBottle -> AppTheme.Fixed.WaterBottle
      ThemeKey.Forest -> AppTheme.Fixed.Forest
      ThemeKey.LifeFloat -> AppTheme.Fixed.LifeFloat
      ThemeKey.Mountain -> AppTheme.Fixed.Mountain
      ThemeKey.Rucksack -> AppTheme.Fixed.Rucksack
      ThemeKey.Dynamic -> AppTheme.Dynamic
      is ThemeKey.Custom -> {
        themingDb.customAppThemeQueries
          .selectById(key.id)
          .awaitAsOneOrNull()
          ?.asDomainModel()
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

  override fun observeCustomThemes(): Flow<List<AppTheme.Fixed.Custom>> {
    return themingDb.customAppThemeQueries
      .selectAll()
      .asFlow()
      .mapToList(dispatcherProvider.databaseRead)
      .map { it.map { dbEntity -> dbEntity.asDomainModel() } }
      .onEach { themes ->
        // Update the local cache
        themeCache.putAll(
          themes.associateBy { it.id },
        )
      }
  }

  override fun setCurrentTheme(theme: AppTheme) {
    currentAppTheme.value = theme
    campfireSettings.themeId = when (theme) {
      AppTheme.Dynamic -> ThemeKey.Dynamic
      AppTheme.Fixed.Forest -> ThemeKey.Forest
      AppTheme.Fixed.LifeFloat -> ThemeKey.LifeFloat
      AppTheme.Fixed.Mountain -> ThemeKey.Mountain
      AppTheme.Fixed.Rucksack -> ThemeKey.Rucksack
      AppTheme.Fixed.Tent -> ThemeKey.Tent
      AppTheme.Fixed.WaterBottle -> ThemeKey.WaterBottle
      is AppTheme.Fixed.Custom -> ThemeKey.Custom(theme.id)
    }
  }

  override suspend fun getCustomTheme(id: String): Result<AppTheme.Fixed.Custom> {
    val cached = themeCache[id]
    return if (cached != null) {
      Result.success(cached)
    } else {
      val diskCached = themingDb.customAppThemeQueries
        .selectById(id)
        .awaitAsOneOrNull()
        ?.asDomainModel()

      if (diskCached != null) {
        themeCache[id] = diskCached
        Result.success(diskCached)
      } else {
        Result.failure(IllegalArgumentException("Unable to find custom theme for the id '$id'"))
      }
    }
  }

  override suspend fun saveCustomTheme(theme: AppTheme.Fixed.Custom) {
    // Cache in memory
    themeCache[theme.id] = theme

    val isCurrentTheme = (campfireSettings.themeId as? ThemeKey.Custom)?.id == theme.id
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

  override suspend fun deleteCustomTheme(id: String) {
    themeCache.remove(id)
    themingDb.customAppThemeQueries.delete(id)
    themingDb.themeQueries.deleteTheme(id)

    val isCurrentTheme = (campfireSettings.themeId as? ThemeKey.Custom)?.id == id
    if (isCurrentTheme) {
      setCurrentTheme(AppTheme.Fixed.Tent)
    }
  }
}

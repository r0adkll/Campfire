package app.campfire.settings

import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.model.UserId
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.ItemDisplayState
import app.campfire.core.settings.SortDirection
import app.campfire.settings.api.CampfireSettings
import app.campfire.settings.api.ThemeKey
import app.campfire.settings.api.ThemeMode
import com.r0adkll.kimchi.annotations.ContributesBinding
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import me.tatarka.inject.annotations.Inject

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = CampfireSettings::class)
@Inject
class CampfireSettingsImpl(
  override val settings: ObservableSettings,
  private val dispatchers: DispatcherProvider,
) : CampfireSettings, AppSettings() {
  private val flowSettings by lazy { settings.toFlowSettings(dispatchers.io) }

  @OptIn(ExperimentalUuidApi::class)
  override var deviceId: String by stringSetting(KEY_DEVICE_ID) { Uuid.random().toString() }
  override var analyticsId: String by stringSetting(KEY_ANALYTICS_ID) { Uuid.random().toString() }

  // These are user opt-in, so default false
  override var hasEverConsented: Boolean by booleanSetting(KEY_HAS_CONSENTED, false)
  override var crashReportingEnabled: Boolean by booleanSetting(KEY_CRASH_REPORTING, true)
  override fun observeCrashReportingEnabled(): Flow<Boolean> {
    return flowSettings.getBooleanFlow(KEY_CRASH_REPORTING, crashReportingEnabled)
  }

  override var analyticReportingEnabled: Boolean by booleanSetting(KEY_ANALYTIC_REPORTING, false)
  override fun observeAnalyticReportingEnabled(): Flow<Boolean> {
    return flowSettings.getBooleanFlow(KEY_ANALYTIC_REPORTING, analyticReportingEnabled)
  }

  override var themeId: ThemeKey by customSetting(
    key = KEY_CURRENT_THEME,
    defaultValue = ThemeKey.Tent,
    getter = { ThemeKey.from(it) },
    setter = { it.storageKey },
  )

  override var themeMode: ThemeMode by enumSetting(KEY_THEME, ThemeMode)
  override fun observeTheme(): Flow<ThemeMode> {
    return flowSettings.getEnumFlow(KEY_THEME, ThemeMode)
  }

  override var libraryItemDisplayState: ItemDisplayState
    by enumSetting(KEY_LIBRARY_ITEM_DISPLAY_STATE, ItemDisplayState)
  override fun observeLibraryItemDisplayState(): Flow<ItemDisplayState> {
    return flowSettings.getEnumFlow(KEY_LIBRARY_ITEM_DISPLAY_STATE, ItemDisplayState)
  }

  override var librarySortMode: ContentSortMode by enumSetting(KEY_SORT_MODE, ContentSortMode.LibraryItemSortMode)
  override fun observeLibrarySortMode(): Flow<ContentSortMode> {
    return flowSettings.getEnumFlow(KEY_SORT_MODE, ContentSortMode.LibraryItemSortMode)
  }

  override var librarySortDirection: SortDirection by enumSetting(KEY_SORT_DIRECTION, SortDirection)
  override fun observeLibrarySortDirection(): Flow<SortDirection> {
    return flowSettings.getEnumFlow(KEY_SORT_DIRECTION, SortDirection)
  }

  override var authorsSortMode: ContentSortMode by enumSetting(KEY_AUTHOR_SORT_MODE, ContentSortMode.AuthorSortMode)
  override fun observeAuthorsSortMode(): Flow<ContentSortMode> {
    return flowSettings.getEnumFlow(KEY_AUTHOR_SORT_MODE, ContentSortMode.AuthorSortMode)
  }

  override var authorsSortDirection: SortDirection by enumSetting(KEY_AUTHORS_SORT_DIRECTION, SortDirection)
  override fun observeAuthorsSortDirection(): Flow<SortDirection> {
    return flowSettings.getEnumFlow(KEY_AUTHORS_SORT_DIRECTION, SortDirection)
  }

  override var seriesSortMode: ContentSortMode by enumSetting(KEY_SERIES_SORT_MODE, ContentSortMode.SeriesSortMode)
  override fun observeSeriesSortMode(): Flow<ContentSortMode> {
    return flowSettings.getEnumFlow(KEY_SERIES_SORT_MODE, ContentSortMode.SeriesSortMode)
  }

  override var seriesSortDirection: SortDirection by enumSetting(KEY_SERIES_SORT_DIRECTION, SortDirection)
  override fun observeSeriesSortDirection(): Flow<SortDirection> {
    return flowSettings.getEnumFlow(KEY_SERIES_SORT_DIRECTION, SortDirection)
  }

  override var currentUserId: UserId? by stringOrNullSetting(KEY_CURRENT_USER_ID)
  override fun observeCurrentUserId(): Flow<UserId?> {
    return flowSettings.getStringOrNullFlow(KEY_CURRENT_USER_ID)
  }

  override var showConfirmDownload: Boolean by booleanSetting(KEY_SHOW_CONFIRM_DOWNLOAD, true)
  override fun observeShowConfirmDownload(): Flow<Boolean> {
    return flowSettings.getBooleanFlow(KEY_SHOW_CONFIRM_DOWNLOAD, true)
  }

  override var hasShownWidgetPinning: Boolean by booleanSetting(KEY_SHOW_WIDGET_PINNING, false)
  override fun observeHasShownWidgetPinning(): Flow<Boolean> {
    return flowSettings.getBooleanFlow(KEY_SHOW_WIDGET_PINNING, false)
  }

  override var showTimeInBook: Boolean by booleanSetting(KEY_SHOW_TIME_IN_BOOK, true)
  override fun observeShowTimeInBook(): Flow<Boolean> {
    return flowSettings.getBooleanFlow(KEY_SHOW_TIME_IN_BOOK, true)
  }
}

internal const val KEY_DEVICE_ID = "pref_device_id"
internal const val KEY_ANALYTICS_ID = "pref_analytics_id"
internal const val KEY_HAS_CONSENTED = "pref_has_consented"
internal const val KEY_CRASH_REPORTING = "pref_crash_reporting"
internal const val KEY_ANALYTIC_REPORTING = "pref_analytic_reporting"
internal const val KEY_CURRENT_THEME = "pref_current_theme"
internal const val KEY_THEME = "pref_theme"
internal const val KEY_LIBRARY_ITEM_DISPLAY_STATE = "pref_library_item_display_state"
internal const val KEY_SORT_MODE = "pref_sort_mode"
internal const val KEY_SORT_DIRECTION = "pref_sort_direction"
internal const val KEY_AUTHOR_SORT_MODE = "pref_authors_sort_mode"
internal const val KEY_AUTHORS_SORT_DIRECTION = "pref_authors_sort_direction"
internal const val KEY_SERIES_SORT_MODE = "pref_series_sort_mode"
internal const val KEY_SERIES_SORT_DIRECTION = "pref_series_sort_direction"
internal const val KEY_CURRENT_USER_ID = "pref_current_user_id"
internal const val KEY_SHOW_CONFIRM_DOWNLOAD = "pref_show_confirm_download"
internal const val KEY_SHOW_WIDGET_PINNING = "pref_show_widget_pinning"
internal const val KEY_SHOW_TIME_IN_BOOK = "pref_show_time_in_book"

// Dead keys
internal const val KEY_USE_DYNAMIC_COLORS = "pref_dynamic_colors"

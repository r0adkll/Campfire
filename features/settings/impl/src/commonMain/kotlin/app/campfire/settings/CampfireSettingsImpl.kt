package app.campfire.settings

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.qualifier.ForScope
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
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import me.tatarka.inject.annotations.Inject

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = CampfireSettings::class)
@Inject
class CampfireSettingsImpl(
  override val settings: ObservableSettings,
  @ForScope(AppScope::class) override val scope: CoroutineScope,
) : CampfireSettings, AppSettings() {

  @OptIn(ExperimentalUuidApi::class)
  override var deviceId: String by stringSetting(KEY_DEVICE_ID) { Uuid.random().toString() }
  override var analyticsId: String by stringSetting(KEY_ANALYTICS_ID) { Uuid.random().toString() }

  // These are user opt-in, so default false
  override var hasEverConsented: Boolean by booleanSetting(KEY_HAS_CONSENTED, false)

  private val crashReportingProperty = booleanSetting(KEY_CRASH_REPORTING, true)
  override var crashReportingEnabled: Boolean by crashReportingProperty
  override fun observeCrashReportingEnabled(): StateFlow<Boolean> = crashReportingProperty.observe()

  private val analyticReportingProperty = booleanSetting(KEY_ANALYTIC_REPORTING, false)
  override var analyticReportingEnabled: Boolean by analyticReportingProperty
  override fun observeAnalyticReportingEnabled(): StateFlow<Boolean> = analyticReportingProperty.observe()

  override var themeId: ThemeKey by customSetting(
    key = KEY_CURRENT_THEME,
    defaultValue = ThemeKey.Tent,
    getter = { ThemeKey.from(it) },
    setter = { it.storageKey },
  )

  private val themeModeProperty = enumSetting(KEY_THEME, ThemeMode)
  override var themeMode: ThemeMode by themeModeProperty
  override fun observeTheme(): StateFlow<ThemeMode> = themeModeProperty.observe()

  private val libraryItemDisplayStateProperty = enumSetting(KEY_LIBRARY_ITEM_DISPLAY_STATE, ItemDisplayState)
  override var libraryItemDisplayState: ItemDisplayState by libraryItemDisplayStateProperty
  override fun observeLibraryItemDisplayState(): StateFlow<ItemDisplayState> = libraryItemDisplayStateProperty.observe()

  private val libraryItemMarqueeProperty = booleanSetting(KEY_LIBRARY_ITEM_MARQUEE, true)
  override var libraryItemMarqueeEnabled: Boolean by libraryItemMarqueeProperty
  override fun observeLibraryItemMarqueeEnabled(): StateFlow<Boolean> = libraryItemMarqueeProperty.observe()

  private val librarySortModeProperty = enumSetting(KEY_SORT_MODE, ContentSortMode.LibraryItemSortMode)
  override var librarySortMode: ContentSortMode by librarySortModeProperty
  override fun observeLibrarySortMode(): StateFlow<ContentSortMode> = librarySortModeProperty.observe()

  private val librarySortDirectionProperty = enumSetting(KEY_SORT_DIRECTION, SortDirection)
  override var librarySortDirection: SortDirection by librarySortDirectionProperty
  override fun observeLibrarySortDirection(): StateFlow<SortDirection> = librarySortDirectionProperty.observe()

  private val authorsSortModeProperty = enumSetting(KEY_AUTHOR_SORT_MODE, ContentSortMode.AuthorSortMode)
  override var authorsSortMode: ContentSortMode by authorsSortModeProperty
  override fun observeAuthorsSortMode(): StateFlow<ContentSortMode> = authorsSortModeProperty.observe()

  private val authorsSortDirectionProperty = enumSetting(KEY_AUTHORS_SORT_DIRECTION, SortDirection)
  override var authorsSortDirection: SortDirection by authorsSortDirectionProperty
  override fun observeAuthorsSortDirection(): StateFlow<SortDirection> = authorsSortDirectionProperty.observe()

  private val seriesSortModeProperty = enumSetting(KEY_SERIES_SORT_MODE, ContentSortMode.SeriesSortMode)
  override var seriesSortMode: ContentSortMode by seriesSortModeProperty
  override fun observeSeriesSortMode(): StateFlow<ContentSortMode> = seriesSortModeProperty.observe()

  private val seriesSortDirectionProperty = enumSetting(KEY_SERIES_SORT_DIRECTION, SortDirection)
  override var seriesSortDirection: SortDirection by seriesSortDirectionProperty
  override fun observeSeriesSortDirection(): StateFlow<SortDirection> = seriesSortDirectionProperty.observe()

  private val currentUserIdProperty = stringOrNullSetting(KEY_CURRENT_USER_ID)
  override var currentUserId: UserId? by currentUserIdProperty
  override fun observeCurrentUserId(): StateFlow<UserId?> = currentUserIdProperty.observe()

  private val showConfirmDownloadProperty = booleanSetting(KEY_SHOW_CONFIRM_DOWNLOAD, true)
  override var showConfirmDownload: Boolean by showConfirmDownloadProperty
  override fun observeShowConfirmDownload(): StateFlow<Boolean> = showConfirmDownloadProperty.observe()

  private val hasShownWidgetPinningProperty = booleanSetting(KEY_SHOW_WIDGET_PINNING, false)
  override var hasShownWidgetPinning: Boolean by hasShownWidgetPinningProperty
  override fun observeHasShownWidgetPinning(): StateFlow<Boolean> = hasShownWidgetPinningProperty.observe()

  private val showTimeInBookProperty = booleanSetting(KEY_SHOW_TIME_IN_BOOK, true)
  override var showTimeInBook: Boolean by showTimeInBookProperty
  override fun observeShowTimeInBook(): StateFlow<Boolean> = showTimeInBookProperty.observe()

  private val lastSeenVersionProperty = stringOrNullSetting(KEY_LAST_SEEN_WHATS_NEW)
  override var lastSeenVersion: String? by lastSeenVersionProperty
  override fun observeLastSeenVersion(): StateFlow<String?> = lastSeenVersionProperty.observe()
}

internal const val KEY_DEVICE_ID = "pref_device_id"
internal const val KEY_ANALYTICS_ID = "pref_analytics_id"
internal const val KEY_HAS_CONSENTED = "pref_has_consented"
internal const val KEY_CRASH_REPORTING = "pref_crash_reporting"
internal const val KEY_ANALYTIC_REPORTING = "pref_analytic_reporting"
internal const val KEY_CURRENT_THEME = "pref_current_theme"
internal const val KEY_THEME = "pref_theme"
internal const val KEY_LIBRARY_ITEM_DISPLAY_STATE = "pref_library_item_display_state"
internal const val KEY_LIBRARY_ITEM_MARQUEE = "pref_library_item_marquee"
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
internal const val KEY_LAST_SEEN_WHATS_NEW = "pref_last_seen_whats_new"

// Dead keys
internal const val KEY_USE_DYNAMIC_COLORS = "pref_dynamic_colors"

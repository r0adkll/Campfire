package app.campfire.settings

import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.model.UserId
import app.campfire.core.settings.ItemDisplayState
import app.campfire.core.settings.SortDirection
import app.campfire.core.settings.SortMode
import app.campfire.settings.api.CampfireSettings
import app.campfire.settings.api.CampfireSettings.Theme
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

  override var theme: Theme by enumSetting(KEY_THEME, Theme)
  override fun observeTheme(): Flow<Theme> {
    return flowSettings.getEnumFlow(KEY_THEME, Theme)
  }

  override var useDynamicColors: Boolean by booleanSetting(KEY_USE_DYNAMIC_COLORS, false)
  override fun observeUseDynamicColors(): Flow<Boolean> {
    return flowSettings.getBooleanFlow(KEY_USE_DYNAMIC_COLORS, false)
  }

  override var libraryItemDisplayState: ItemDisplayState
    by enumSetting(KEY_LIBRARY_ITEM_DISPLAY_STATE, ItemDisplayState)
  override fun observeLibraryItemDisplayState(): Flow<ItemDisplayState> {
    return flowSettings.getEnumFlow(KEY_LIBRARY_ITEM_DISPLAY_STATE, ItemDisplayState)
  }

  override var sortMode: SortMode by enumSetting(KEY_SORT_MODE, SortMode)
  override fun observeSortMode(): Flow<SortMode> {
    return flowSettings.getEnumFlow(KEY_SORT_MODE, SortMode)
  }

  override var sortDirection: SortDirection by enumSetting(KEY_SORT_DIRECTION, SortDirection)
  override fun observeSortDirection(): Flow<SortDirection> {
    return flowSettings.getEnumFlow(KEY_SORT_DIRECTION, SortDirection)
  }

  override var currentUserId: UserId? by stringOrNullSetting(KEY_CURRENT_USER_ID)
  override fun observeCurrentUserId(): Flow<UserId?> {
    return flowSettings.getStringOrNullFlow(KEY_CURRENT_USER_ID)
  }
}

internal const val KEY_DEVICE_ID = "pref_device_id"
internal const val KEY_THEME = "pref_theme"
internal const val KEY_USE_DYNAMIC_COLORS = "pref_dynamic_colors"
internal const val KEY_LIBRARY_ITEM_DISPLAY_STATE = "pref_library_item_display_state"
internal const val KEY_SORT_MODE = "pref_sort_mode"
internal const val KEY_SORT_DIRECTION = "pref_sort_direction"
internal const val KEY_CURRENT_USER_ID = "pref_current_user_id"

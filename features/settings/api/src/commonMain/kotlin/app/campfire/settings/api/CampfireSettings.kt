package app.campfire.settings.api

import app.campfire.core.model.UserId
import app.campfire.core.settings.EnumSetting
import app.campfire.core.settings.EnumSettingProvider
import app.campfire.core.settings.ItemDisplayState
import app.campfire.core.settings.SortDirection
import app.campfire.core.settings.SortMode
import kotlinx.coroutines.flow.Flow

interface CampfireSettings {

  var deviceId: String
  var analyticsId: String

  var hasEverConsented: Boolean

  var crashReportingEnabled: Boolean
  fun observeCrashReportingEnabled(): Flow<Boolean>

  var analyticReportingEnabled: Boolean
  fun observeAnalyticReportingEnabled(): Flow<Boolean>

  var theme: Theme
  fun observeTheme(): Flow<Theme>

  var useDynamicColors: Boolean
  fun observeUseDynamicColors(): Flow<Boolean>

  var libraryItemDisplayState: ItemDisplayState
  fun observeLibraryItemDisplayState(): Flow<ItemDisplayState>

  var sortMode: SortMode
  fun observeSortMode(): Flow<SortMode>

  var sortDirection: SortDirection
  fun observeSortDirection(): Flow<SortDirection>

  var currentUserId: UserId?
  fun observeCurrentUserId(): Flow<UserId?>

  var showConfirmDownload: Boolean
  fun observeShowConfirmDownload(): Flow<Boolean>

  var hasShownWidgetPinning: Boolean
  fun observeHasShownWidgetPinning(): Flow<Boolean>

  var showTimeInBook: Boolean
  fun observeShowTimeInBook(): Flow<Boolean>

  enum class Theme(override val storageKey: String) : EnumSetting {
    LIGHT("light"),
    DARK("dark"),
    SYSTEM("system"),
    ;

    companion object : EnumSettingProvider<Theme> {
      override fun fromStorageKey(key: String?): Theme {
        return values().find { it.storageKey == key } ?: SYSTEM
      }
    }
  }
}

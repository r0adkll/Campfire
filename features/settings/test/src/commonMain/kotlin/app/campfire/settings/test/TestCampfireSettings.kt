package app.campfire.settings.test

import app.campfire.core.model.UserId
import app.campfire.core.settings.ItemDisplayState
import app.campfire.core.settings.SortDirection
import app.campfire.core.settings.SortMode
import app.campfire.settings.api.CampfireSettings
import app.campfire.settings.api.ThemeKey
import app.campfire.settings.api.ThemeMode
import kotlinx.coroutines.flow.Flow

class TestCampfireSettings : TestSettings(), CampfireSettings {

  override var deviceId: String by string()
  override var analyticsId: String by string()
  override var hasEverConsented: Boolean by boolean()
  override var crashReportingEnabled: Boolean by boolean()

  override var themeId: ThemeKey = ThemeKey.Tent

  override fun observeCrashReportingEnabled(): Flow<Boolean> =
    observeBoolean(::crashReportingEnabled)

  override var analyticReportingEnabled: Boolean by boolean()
  override fun observeAnalyticReportingEnabled(): Flow<Boolean> =
    observeBoolean(::analyticReportingEnabled)

  override var themeMode: ThemeMode by enum()
  override fun observeTheme(): Flow<ThemeMode> =
    observeEnum(::themeMode)

  override var libraryItemDisplayState: ItemDisplayState by enum()
  override fun observeLibraryItemDisplayState(): Flow<ItemDisplayState> =
    observeEnum(::libraryItemDisplayState)

  override var sortMode: SortMode by enum()
  override fun observeSortMode(): Flow<SortMode> =
    observeEnum(::sortMode)

  override var sortDirection: SortDirection by enum()
  override fun observeSortDirection(): Flow<SortDirection> =
    observeEnum(::sortDirection)

  override var currentUserId: UserId? by stringOrNull()
  override fun observeCurrentUserId(): Flow<UserId?> =
    observeStringOrNull(::currentUserId)

  override var showConfirmDownload: Boolean by boolean()
  override fun observeShowConfirmDownload(): Flow<Boolean> =
    observeBoolean(::showConfirmDownload)

  override var hasShownWidgetPinning: Boolean by boolean()
  override fun observeHasShownWidgetPinning(): Flow<Boolean> =
    observeBoolean(::hasShownWidgetPinning)

  override var showTimeInBook: Boolean by boolean()
  override fun observeShowTimeInBook(): Flow<Boolean> =
    observeBoolean(::showTimeInBook)
}

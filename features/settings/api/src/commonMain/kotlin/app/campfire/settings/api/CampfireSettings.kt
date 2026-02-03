package app.campfire.settings.api

import app.campfire.core.model.UserId
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.ItemDisplayState
import app.campfire.core.settings.SortDirection
import kotlinx.coroutines.flow.Flow

// TODO: Break-up the CampfireSettings monolith into appScope/userScope layers
interface CampfireSettings {

  var deviceId: String
  var analyticsId: String

  var hasEverConsented: Boolean

  var crashReportingEnabled: Boolean
  fun observeCrashReportingEnabled(): Flow<Boolean>

  var analyticReportingEnabled: Boolean
  fun observeAnalyticReportingEnabled(): Flow<Boolean>

  var themeId: ThemeKey

  var themeMode: ThemeMode
  fun observeTheme(): Flow<ThemeMode>

  var libraryItemDisplayState: ItemDisplayState
  fun observeLibraryItemDisplayState(): Flow<ItemDisplayState>

  var librarySortMode: ContentSortMode
  fun observeLibrarySortMode(): Flow<ContentSortMode>

  var librarySortDirection: SortDirection
  fun observeLibrarySortDirection(): Flow<SortDirection>

  var authorsSortMode: ContentSortMode
  fun observeAuthorsSortMode(): Flow<ContentSortMode>

  var authorsSortDirection: SortDirection
  fun observeAuthorsSortDirection(): Flow<SortDirection>

  var seriesSortMode: ContentSortMode
  fun observeSeriesSortMode(): Flow<ContentSortMode>

  var seriesSortDirection: SortDirection
  fun observeSeriesSortDirection(): Flow<SortDirection>

  var currentUserId: UserId?
  fun observeCurrentUserId(): Flow<UserId?>

  var showConfirmDownload: Boolean
  fun observeShowConfirmDownload(): Flow<Boolean>

  var hasShownWidgetPinning: Boolean
  fun observeHasShownWidgetPinning(): Flow<Boolean>

  var showTimeInBook: Boolean
  fun observeShowTimeInBook(): Flow<Boolean>

  var lastSeenVersion: String?
  fun observeLastSeenVersion(): Flow<String?>
}

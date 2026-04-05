package app.campfire.settings.api

import app.campfire.core.model.UserId
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.ItemDisplayState
import app.campfire.core.settings.SortDirection
import kotlinx.coroutines.flow.StateFlow

// TODO: Break-up the CampfireSettings monolith into appScope/userScope layers
interface CampfireSettings {

  var deviceId: String
  var analyticsId: String

  var hasEverConsented: Boolean

  var crashReportingEnabled: Boolean
  fun observeCrashReportingEnabled(): StateFlow<Boolean>

  var analyticReportingEnabled: Boolean
  fun observeAnalyticReportingEnabled(): StateFlow<Boolean>

  var themeId: ThemeKey

  var themeMode: ThemeMode
  fun observeTheme(): StateFlow<ThemeMode>

  var libraryItemDisplayState: ItemDisplayState
  fun observeLibraryItemDisplayState(): StateFlow<ItemDisplayState>

  var libraryItemMarqueeEnabled: Boolean
  fun observeLibraryItemMarqueeEnabled(): StateFlow<Boolean>

  var librarySortMode: ContentSortMode
  fun observeLibrarySortMode(): StateFlow<ContentSortMode>

  var librarySortDirection: SortDirection
  fun observeLibrarySortDirection(): StateFlow<SortDirection>

  var authorsSortMode: ContentSortMode
  fun observeAuthorsSortMode(): StateFlow<ContentSortMode>

  var authorsSortDirection: SortDirection
  fun observeAuthorsSortDirection(): StateFlow<SortDirection>

  var seriesSortMode: ContentSortMode
  fun observeSeriesSortMode(): StateFlow<ContentSortMode>

  var seriesSortDirection: SortDirection
  fun observeSeriesSortDirection(): StateFlow<SortDirection>

  var currentUserId: UserId?
  fun observeCurrentUserId(): StateFlow<UserId?>

  var showConfirmDownload: Boolean
  fun observeShowConfirmDownload(): StateFlow<Boolean>

  var hasShownWidgetPinning: Boolean
  fun observeHasShownWidgetPinning(): StateFlow<Boolean>

  var showTimeInBook: Boolean
  fun observeShowTimeInBook(): StateFlow<Boolean>

  var lastSeenVersion: String?
  fun observeLastSeenVersion(): StateFlow<String?>
}

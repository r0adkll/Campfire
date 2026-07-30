// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import app.campfire.core.model.UserId
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.GroupDisplayState
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

  var seriesDisplayState: GroupDisplayState
  fun observeSeriesDisplayState(): StateFlow<GroupDisplayState>

  var collectionsDisplayState: GroupDisplayState
  fun observeCollectionsDisplayState(): StateFlow<GroupDisplayState>

  var playlistsDisplayState: GroupDisplayState
  fun observePlaylistsDisplayState(): StateFlow<GroupDisplayState>

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

  /**
   * When `true`, the user has dismissed the app update sign-in prompt and it should
   * no longer be shown.
   */
  var appUpdateSignInDismissed: Boolean
  fun observeAppUpdateSignInDismissed(): StateFlow<Boolean>

  /**
   * The versionCode of the last app update the user dismissed from the update widget.
   * The widget stays hidden for that release but shows again for a different one.
   * `0` when no update has been dismissed.
   */
  var appUpdateDismissedVersionCode: Long
  fun observeAppUpdateDismissedVersionCode(): StateFlow<Long>

  /**
   * When `true`, the realtime Socket.IO connection to the ABS server is enabled and the client
   * receives live updates (library/series/collection/podcast changes, media progress, etc.).
   * When `false`, the socket stays disconnected and [app.campfire.socket.SocketState.Disabled]
   * is exposed so UI indicators can hide.
   *
   * Defaults to `true`.
   */
  var socketEnabled: Boolean
  fun observeSocketEnabled(): StateFlow<Boolean>
}

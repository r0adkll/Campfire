// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.test

import app.campfire.core.model.UserId
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.GroupDisplayState
import app.campfire.core.settings.ItemDisplayState
import app.campfire.core.settings.SortDirection
import app.campfire.settings.api.CampfireSettings
import app.campfire.settings.api.ThemeKey
import app.campfire.settings.api.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class TestCampfireSettings(
  private val testScope: CoroutineScope = TestScope(UnconfinedTestDispatcher()),
) : TestSettings(), CampfireSettings {

  override var deviceId: String by string()
  override var analyticsId: String by string()
  override var hasEverConsented: Boolean by boolean()
  override var crashReportingEnabled: Boolean by boolean()

  override var themeId: ThemeKey = ThemeKey.Tent

  override fun observeCrashReportingEnabled(): StateFlow<Boolean> =
    observeBoolean(::crashReportingEnabled)
      .stateIn(testScope, SharingStarted.Lazily, crashReportingEnabled)

  override var analyticReportingEnabled: Boolean by boolean()
  override fun observeAnalyticReportingEnabled(): StateFlow<Boolean> =
    observeBoolean(::analyticReportingEnabled)
      .stateIn(testScope, SharingStarted.Lazily, analyticReportingEnabled)

  override var socketEnabled: Boolean by boolean()
  override fun observeSocketEnabled(): StateFlow<Boolean> =
    observeBoolean(::socketEnabled)
      .stateIn(testScope, SharingStarted.Lazily, socketEnabled)

  override var themeMode: ThemeMode by enum()
  override fun observeTheme(): StateFlow<ThemeMode> =
    observeEnum<ThemeMode>(::themeMode)
      .stateIn(testScope, SharingStarted.Lazily, themeMode)

  override var libraryItemDisplayState: ItemDisplayState by enum()
  override fun observeLibraryItemDisplayState(): StateFlow<ItemDisplayState> =
    observeEnum<ItemDisplayState>(::libraryItemDisplayState)
      .stateIn(testScope, SharingStarted.Lazily, libraryItemDisplayState)

  override var libraryItemMarqueeEnabled: Boolean by boolean()
  override fun observeLibraryItemMarqueeEnabled(): StateFlow<Boolean> =
    observeBoolean(::libraryItemMarqueeEnabled)
      .stateIn(testScope, SharingStarted.Lazily, libraryItemMarqueeEnabled)

  override var librarySortMode: ContentSortMode by enum()
  override fun observeLibrarySortMode(): StateFlow<ContentSortMode> =
    observeEnum<ContentSortMode>(::librarySortMode)
      .stateIn(testScope, SharingStarted.Lazily, librarySortMode)

  override var librarySortDirection: SortDirection by enum()
  override fun observeLibrarySortDirection(): StateFlow<SortDirection> =
    observeEnum<SortDirection>(::librarySortDirection)
      .stateIn(testScope, SharingStarted.Lazily, librarySortDirection)

  override var authorsSortMode: ContentSortMode by enum()
  override fun observeAuthorsSortMode(): StateFlow<ContentSortMode> =
    observeEnum<ContentSortMode>(::authorsSortMode)
      .stateIn(testScope, SharingStarted.Lazily, authorsSortMode)

  override var authorsSortDirection: SortDirection by enum()
  override fun observeAuthorsSortDirection(): StateFlow<SortDirection> =
    observeEnum<SortDirection>(::authorsSortDirection)
      .stateIn(testScope, SharingStarted.Lazily, authorsSortDirection)

  override var seriesSortMode: ContentSortMode by enum()
  override fun observeSeriesSortMode(): StateFlow<ContentSortMode> =
    observeEnum<ContentSortMode>(::seriesSortMode)
      .stateIn(testScope, SharingStarted.Lazily, seriesSortMode)

  override var seriesSortDirection: SortDirection by enum()
  override fun observeSeriesSortDirection(): StateFlow<SortDirection> =
    observeEnum<SortDirection>(::seriesSortDirection)
      .stateIn(testScope, SharingStarted.Lazily, seriesSortDirection)

  override var seriesDisplayState: GroupDisplayState by enum()
  override fun observeSeriesDisplayState(): StateFlow<GroupDisplayState> =
    observeEnum<GroupDisplayState>(::seriesDisplayState)
      .stateIn(testScope, SharingStarted.Lazily, seriesDisplayState)

  override var collectionsDisplayState: GroupDisplayState by enum()
  override fun observeCollectionsDisplayState(): StateFlow<GroupDisplayState> =
    observeEnum<GroupDisplayState>(::collectionsDisplayState)
      .stateIn(testScope, SharingStarted.Lazily, collectionsDisplayState)

  override var playlistsDisplayState: GroupDisplayState by enum()
  override fun observePlaylistsDisplayState(): StateFlow<GroupDisplayState> =
    observeEnum<GroupDisplayState>(::playlistsDisplayState)
      .stateIn(testScope, SharingStarted.Lazily, playlistsDisplayState)

  override var currentUserId: UserId? by stringOrNull()
  override fun observeCurrentUserId(): StateFlow<UserId?> =
    observeStringOrNull(::currentUserId)
      .stateIn(testScope, SharingStarted.Lazily, currentUserId)

  override var showConfirmDownload: Boolean by boolean()
  override fun observeShowConfirmDownload(): StateFlow<Boolean> =
    observeBoolean(::showConfirmDownload)
      .stateIn(testScope, SharingStarted.Lazily, showConfirmDownload)

  override var hasShownWidgetPinning: Boolean by boolean()
  override fun observeHasShownWidgetPinning(): StateFlow<Boolean> =
    observeBoolean(::hasShownWidgetPinning)
      .stateIn(testScope, SharingStarted.Lazily, hasShownWidgetPinning)

  override var showTimeInBook: Boolean by boolean()
  override fun observeShowTimeInBook(): StateFlow<Boolean> =
    observeBoolean(::showTimeInBook)
      .stateIn(testScope, SharingStarted.Lazily, showTimeInBook)

  override var lastSeenVersion: String? by stringOrNull()
  override fun observeLastSeenVersion(): StateFlow<String?> =
    observeStringOrNull(::lastSeenVersion)
      .stateIn(testScope, SharingStarted.Lazily, lastSeenVersion)

  override var appUpdateSignInDismissed: Boolean by boolean()
  override fun observeAppUpdateSignInDismissed(): StateFlow<Boolean> =
    observeBoolean(::appUpdateSignInDismissed)
      .stateIn(testScope, SharingStarted.Lazily, appUpdateSignInDismissed)

  override var appUpdateDismissedVersionCode: Long by long()
  override fun observeAppUpdateDismissedVersionCode(): StateFlow<Long> =
    observeLong(::appUpdateDismissedVersionCode)
      .stateIn(testScope, SharingStarted.Lazily, appUpdateDismissedVersionCode)
}

// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.GroupDisplayState
import app.campfire.core.settings.ItemDisplayState
import app.campfire.core.settings.SortDirection
import app.campfire.settings.api.LibraryViewSettings
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class, binding = binding<LibraryViewSettings>())
@Inject
class LibraryViewSettingsImpl(
  @UserSettings override val settings: ObservableSettings,
  @ForScope(UserScope::class) private val scopeHolder: CoroutineScopeHolder,
) : LibraryViewSettings, AppSettings() {

  // Observers end with the user graph
  override val scope: CoroutineScope get() = scopeHolder.get()

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

  private val seriesDisplayStateProperty = enumSetting(KEY_SERIES_DISPLAY_STATE, GroupDisplayState)
  override var seriesDisplayState: GroupDisplayState by seriesDisplayStateProperty
  override fun observeSeriesDisplayState(): StateFlow<GroupDisplayState> = seriesDisplayStateProperty.observe()

  private val collectionsDisplayStateProperty = enumSetting(KEY_COLLECTIONS_DISPLAY_STATE, GroupDisplayState)
  override var collectionsDisplayState: GroupDisplayState by collectionsDisplayStateProperty
  override fun observeCollectionsDisplayState(): StateFlow<GroupDisplayState> =
    collectionsDisplayStateProperty.observe()

  private val playlistsDisplayStateProperty = enumSetting(KEY_PLAYLISTS_DISPLAY_STATE, GroupDisplayState)
  override var playlistsDisplayState: GroupDisplayState by playlistsDisplayStateProperty
  override fun observePlaylistsDisplayState(): StateFlow<GroupDisplayState> =
    playlistsDisplayStateProperty.observe()

  private val showConfirmDownloadProperty = booleanSetting(KEY_SHOW_CONFIRM_DOWNLOAD, true)
  override var showConfirmDownload: Boolean by showConfirmDownloadProperty
  override fun observeShowConfirmDownload(): StateFlow<Boolean> = showConfirmDownloadProperty.observe()

  private val showTimeInBookProperty = booleanSetting(KEY_SHOW_TIME_IN_BOOK, true)
  override var showTimeInBook: Boolean by showTimeInBookProperty
  override fun observeShowTimeInBook(): StateFlow<Boolean> = showTimeInBookProperty.observe()
}

internal const val KEY_LIBRARY_ITEM_DISPLAY_STATE = "pref_library_item_display_state"
internal const val KEY_LIBRARY_ITEM_MARQUEE = "pref_library_item_marquee"
internal const val KEY_SORT_MODE = "pref_sort_mode"
internal const val KEY_SORT_DIRECTION = "pref_sort_direction"
internal const val KEY_AUTHOR_SORT_MODE = "pref_authors_sort_mode"
internal const val KEY_AUTHORS_SORT_DIRECTION = "pref_authors_sort_direction"
internal const val KEY_SERIES_SORT_MODE = "pref_series_sort_mode"
internal const val KEY_SERIES_SORT_DIRECTION = "pref_series_sort_direction"
internal const val KEY_SERIES_DISPLAY_STATE = "pref_series_display_state"
internal const val KEY_COLLECTIONS_DISPLAY_STATE = "pref_collections_display_state"
internal const val KEY_PLAYLISTS_DISPLAY_STATE = "pref_playlists_display_state"
internal const val KEY_SHOW_CONFIRM_DOWNLOAD = "pref_show_confirm_download"
internal const val KEY_SHOW_TIME_IN_BOOK = "pref_show_time_in_book"

/** The library view settings stored as strings, for copying into a new account's settings. */
internal val LibraryViewStringKeys = listOf(
  KEY_LIBRARY_ITEM_DISPLAY_STATE,
  KEY_SORT_MODE,
  KEY_SORT_DIRECTION,
  KEY_AUTHOR_SORT_MODE,
  KEY_AUTHORS_SORT_DIRECTION,
  KEY_SERIES_SORT_MODE,
  KEY_SERIES_SORT_DIRECTION,
  KEY_SERIES_DISPLAY_STATE,
  KEY_COLLECTIONS_DISPLAY_STATE,
  KEY_PLAYLISTS_DISPLAY_STATE,
)

/** The library view settings stored as booleans. */
internal val LibraryViewBooleanKeys = listOf(
  KEY_LIBRARY_ITEM_MARQUEE,
  KEY_SHOW_CONFIRM_DOWNLOAD,
  KEY_SHOW_TIME_IN_BOOK,
)

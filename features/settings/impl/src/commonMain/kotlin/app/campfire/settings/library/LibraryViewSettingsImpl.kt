// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.library

import app.campfire.core.di.AppScope
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.GroupDisplayState
import app.campfire.core.settings.ItemDisplayState
import app.campfire.core.settings.SortDirection
import app.campfire.settings.api.LibraryViewSettings
import app.campfire.settings.store.AppSettings
import app.campfire.settings.store.SettingsStore
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.Flow

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<LibraryViewSettings>())
@Inject
class LibraryViewSettingsImpl(
  override val store: SettingsStore,
) : LibraryViewSettings, AppSettings() {

  private val libraryItemDisplayStateProperty = enumSetting(KEY_LIBRARY_ITEM_DISPLAY_STATE, ItemDisplayState)
  override fun setLibraryItemDisplayState(value: ItemDisplayState) = libraryItemDisplayStateProperty.set(value)
  override fun observeLibraryItemDisplayState(): Flow<ItemDisplayState> = libraryItemDisplayStateProperty.observe()

  private val libraryItemMarqueeProperty =
    booleanSetting(KEY_LIBRARY_ITEM_MARQUEE, LibraryViewSettings.DEFAULT_ITEM_MARQUEE_ENABLED)
  override fun setLibraryItemMarqueeEnabled(value: Boolean) = libraryItemMarqueeProperty.set(value)
  override fun observeLibraryItemMarqueeEnabled(): Flow<Boolean> = libraryItemMarqueeProperty.observe()

  private val librarySortModeProperty = enumSetting(KEY_SORT_MODE, ContentSortMode.LibraryItemSortMode)
  override fun setLibrarySortMode(value: ContentSortMode) = librarySortModeProperty.set(value)
  override fun observeLibrarySortMode(): Flow<ContentSortMode> = librarySortModeProperty.observe()

  private val librarySortDirectionProperty = enumSetting(KEY_SORT_DIRECTION, SortDirection)
  override fun setLibrarySortDirection(value: SortDirection) = librarySortDirectionProperty.set(value)
  override fun observeLibrarySortDirection(): Flow<SortDirection> = librarySortDirectionProperty.observe()

  private val authorsSortModeProperty = enumSetting(KEY_AUTHOR_SORT_MODE, ContentSortMode.AuthorSortMode)
  override fun setAuthorsSortMode(value: ContentSortMode) = authorsSortModeProperty.set(value)
  override fun observeAuthorsSortMode(): Flow<ContentSortMode> = authorsSortModeProperty.observe()

  private val authorsSortDirectionProperty = enumSetting(KEY_AUTHORS_SORT_DIRECTION, SortDirection)
  override fun setAuthorsSortDirection(value: SortDirection) = authorsSortDirectionProperty.set(value)
  override fun observeAuthorsSortDirection(): Flow<SortDirection> = authorsSortDirectionProperty.observe()

  private val seriesSortModeProperty = enumSetting(KEY_SERIES_SORT_MODE, ContentSortMode.SeriesSortMode)
  override fun setSeriesSortMode(value: ContentSortMode) = seriesSortModeProperty.set(value)
  override fun observeSeriesSortMode(): Flow<ContentSortMode> = seriesSortModeProperty.observe()

  private val seriesSortDirectionProperty = enumSetting(KEY_SERIES_SORT_DIRECTION, SortDirection)
  override fun setSeriesSortDirection(value: SortDirection) = seriesSortDirectionProperty.set(value)
  override fun observeSeriesSortDirection(): Flow<SortDirection> = seriesSortDirectionProperty.observe()

  private val seriesDisplayStateProperty = enumSetting(KEY_SERIES_DISPLAY_STATE, GroupDisplayState)
  override fun setSeriesDisplayState(value: GroupDisplayState) = seriesDisplayStateProperty.set(value)
  override fun observeSeriesDisplayState(): Flow<GroupDisplayState> = seriesDisplayStateProperty.observe()

  private val collectionsDisplayStateProperty = enumSetting(KEY_COLLECTIONS_DISPLAY_STATE, GroupDisplayState)
  override fun setCollectionsDisplayState(value: GroupDisplayState) = collectionsDisplayStateProperty.set(value)
  override fun observeCollectionsDisplayState(): Flow<GroupDisplayState> =
    collectionsDisplayStateProperty.observe()

  private val playlistsDisplayStateProperty = enumSetting(KEY_PLAYLISTS_DISPLAY_STATE, GroupDisplayState)
  override fun setPlaylistsDisplayState(value: GroupDisplayState) = playlistsDisplayStateProperty.set(value)
  override fun observePlaylistsDisplayState(): Flow<GroupDisplayState> =
    playlistsDisplayStateProperty.observe()

  private val showConfirmDownloadProperty =
    booleanSetting(KEY_SHOW_CONFIRM_DOWNLOAD, LibraryViewSettings.DEFAULT_SHOW_CONFIRM_DOWNLOAD)
  override fun setShowConfirmDownload(value: Boolean) = showConfirmDownloadProperty.set(value)
  override fun observeShowConfirmDownload(): Flow<Boolean> = showConfirmDownloadProperty.observe()

  private val showTimeInBookProperty =
    booleanSetting(KEY_SHOW_TIME_IN_BOOK, LibraryViewSettings.DEFAULT_SHOW_TIME_IN_BOOK)
  override fun setShowTimeInBook(value: Boolean) = showTimeInBookProperty.set(value)
  override fun observeShowTimeInBook(): Flow<Boolean> = showTimeInBookProperty.observe()
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

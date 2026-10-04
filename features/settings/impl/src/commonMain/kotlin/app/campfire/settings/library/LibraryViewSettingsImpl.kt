// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.library

import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.GroupDisplayState
import app.campfire.core.settings.ItemDisplayState
import app.campfire.core.settings.SortDirection
import app.campfire.settings.api.LibraryViewSettings
import app.campfire.settings.store.AppSettings
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<LibraryViewSettings>())
@Inject
class LibraryViewSettingsImpl(
  override val settings: ObservableSettings,
  @ForScope(AppScope::class) override val scope: CoroutineScope,
) : LibraryViewSettings, AppSettings() {

  private val libraryItemDisplayStateProperty = enumSetting(KEY_LIBRARY_ITEM_DISPLAY_STATE, ItemDisplayState)
  override val libraryItemDisplayState: ItemDisplayState by libraryItemDisplayStateProperty
  override fun setLibraryItemDisplayState(value: ItemDisplayState) = libraryItemDisplayStateProperty.set(value)
  override fun observeLibraryItemDisplayState(): StateFlow<ItemDisplayState> = libraryItemDisplayStateProperty.observe()

  private val libraryItemMarqueeProperty = booleanSetting(KEY_LIBRARY_ITEM_MARQUEE, true)
  override val libraryItemMarqueeEnabled: Boolean by libraryItemMarqueeProperty
  override fun setLibraryItemMarqueeEnabled(value: Boolean) = libraryItemMarqueeProperty.set(value)
  override fun observeLibraryItemMarqueeEnabled(): StateFlow<Boolean> = libraryItemMarqueeProperty.observe()

  private val librarySortModeProperty = enumSetting(KEY_SORT_MODE, ContentSortMode.LibraryItemSortMode)
  override val librarySortMode: ContentSortMode by librarySortModeProperty
  override fun setLibrarySortMode(value: ContentSortMode) = librarySortModeProperty.set(value)
  override fun observeLibrarySortMode(): StateFlow<ContentSortMode> = librarySortModeProperty.observe()

  private val librarySortDirectionProperty = enumSetting(KEY_SORT_DIRECTION, SortDirection)
  override val librarySortDirection: SortDirection by librarySortDirectionProperty
  override fun setLibrarySortDirection(value: SortDirection) = librarySortDirectionProperty.set(value)
  override fun observeLibrarySortDirection(): StateFlow<SortDirection> = librarySortDirectionProperty.observe()

  private val authorsSortModeProperty = enumSetting(KEY_AUTHOR_SORT_MODE, ContentSortMode.AuthorSortMode)
  override val authorsSortMode: ContentSortMode by authorsSortModeProperty
  override fun setAuthorsSortMode(value: ContentSortMode) = authorsSortModeProperty.set(value)
  override fun observeAuthorsSortMode(): StateFlow<ContentSortMode> = authorsSortModeProperty.observe()

  private val authorsSortDirectionProperty = enumSetting(KEY_AUTHORS_SORT_DIRECTION, SortDirection)
  override val authorsSortDirection: SortDirection by authorsSortDirectionProperty
  override fun setAuthorsSortDirection(value: SortDirection) = authorsSortDirectionProperty.set(value)
  override fun observeAuthorsSortDirection(): StateFlow<SortDirection> = authorsSortDirectionProperty.observe()

  private val seriesSortModeProperty = enumSetting(KEY_SERIES_SORT_MODE, ContentSortMode.SeriesSortMode)
  override val seriesSortMode: ContentSortMode by seriesSortModeProperty
  override fun setSeriesSortMode(value: ContentSortMode) = seriesSortModeProperty.set(value)
  override fun observeSeriesSortMode(): StateFlow<ContentSortMode> = seriesSortModeProperty.observe()

  private val seriesSortDirectionProperty = enumSetting(KEY_SERIES_SORT_DIRECTION, SortDirection)
  override val seriesSortDirection: SortDirection by seriesSortDirectionProperty
  override fun setSeriesSortDirection(value: SortDirection) = seriesSortDirectionProperty.set(value)
  override fun observeSeriesSortDirection(): StateFlow<SortDirection> = seriesSortDirectionProperty.observe()

  private val seriesDisplayStateProperty = enumSetting(KEY_SERIES_DISPLAY_STATE, GroupDisplayState)
  override val seriesDisplayState: GroupDisplayState by seriesDisplayStateProperty
  override fun setSeriesDisplayState(value: GroupDisplayState) = seriesDisplayStateProperty.set(value)
  override fun observeSeriesDisplayState(): StateFlow<GroupDisplayState> = seriesDisplayStateProperty.observe()

  private val collectionsDisplayStateProperty = enumSetting(KEY_COLLECTIONS_DISPLAY_STATE, GroupDisplayState)
  override val collectionsDisplayState: GroupDisplayState by collectionsDisplayStateProperty
  override fun setCollectionsDisplayState(value: GroupDisplayState) = collectionsDisplayStateProperty.set(value)
  override fun observeCollectionsDisplayState(): StateFlow<GroupDisplayState> =
    collectionsDisplayStateProperty.observe()

  private val playlistsDisplayStateProperty = enumSetting(KEY_PLAYLISTS_DISPLAY_STATE, GroupDisplayState)
  override val playlistsDisplayState: GroupDisplayState by playlistsDisplayStateProperty
  override fun setPlaylistsDisplayState(value: GroupDisplayState) = playlistsDisplayStateProperty.set(value)
  override fun observePlaylistsDisplayState(): StateFlow<GroupDisplayState> =
    playlistsDisplayStateProperty.observe()

  private val showConfirmDownloadProperty = booleanSetting(KEY_SHOW_CONFIRM_DOWNLOAD, true)
  override val showConfirmDownload: Boolean by showConfirmDownloadProperty
  override fun setShowConfirmDownload(value: Boolean) = showConfirmDownloadProperty.set(value)
  override fun observeShowConfirmDownload(): StateFlow<Boolean> = showConfirmDownloadProperty.observe()

  private val showTimeInBookProperty = booleanSetting(KEY_SHOW_TIME_IN_BOOK, true)
  override val showTimeInBook: Boolean by showTimeInBookProperty
  override fun setShowTimeInBook(value: Boolean) = showTimeInBookProperty.set(value)
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

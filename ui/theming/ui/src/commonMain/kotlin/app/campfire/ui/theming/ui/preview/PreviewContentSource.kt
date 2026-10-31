// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.preview

import app.campfire.core.di.UserScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.MediaType
import app.campfire.core.model.ShelfEntity
import app.campfire.home.api.HomeRepository
import app.campfire.home.api.model.Shelf
import app.campfire.home.api.model.ShelfIds
import app.campfire.libraries.api.LibraryRepository
import app.campfire.settings.api.LibraryViewSettings
import app.campfire.settings.api.ThemeSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/** Feeds the theme preview what it draws. */
fun interface PreviewContentSource {
  fun observe(): Flow<PreviewContent>
}

/**
 * The user's own Home shelves, library size and layout, and whether the player uses cover
 * colors. It reads what's cached and never waits on the server, so the preview shows
 * samples until there's something to show.
 */
@ContributesBinding(UserScope::class)
@Inject
class DefaultPreviewContentSource(
  private val homeRepository: HomeRepository,
  private val libraryRepository: LibraryRepository,
  private val libraryViewSettings: LibraryViewSettings,
  private val themeSettings: ThemeSettings,
) : PreviewContentSource {

  override fun observe(): Flow<PreviewContent> = combine(
    observeLibrary(),
    themeSettings.observeDynamicallyThemePlayback(),
  ) { library, playerUsesCoverColors ->
    PreviewContent(library = library, playerUsesCoverColors = playerUsesCoverColors)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private fun observeLibrary(): Flow<PreviewLibrary> {
    val shelves = homeRepository.observeHomeFeed()
      .map { it.dataOrNull.orEmpty() }
      .orFallback("the home feed", emptyList())
      .distinctUntilChanged()
      .flatMapLatest { shelves ->
        combine(
          observeShelf(shelves.serverShelf(ShelfIds.ContinueListening)),
          observeShelf(
            shelves.serverShelf(ShelfIds.RecentlyAdded)
              ?: shelves.serverShelf(ShelfIds.EpisodesRecentlyAdded),
          ),
        ) { continueListening, recentlyAdded -> continueListening to recentlyAdded }
      }

    val layout = combine(
      libraryViewSettings.observeLibraryItemDisplayState(),
      libraryViewSettings.observeLibrarySortMode(),
      libraryViewSettings.observeLibrarySortDirection(),
    ) { displayState, sortMode, sortDirection -> Triple(displayState, sortMode, sortDirection) }

    val itemCount = layout.flatMapLatest { (_, sortMode, sortDirection) ->
      libraryRepository.observeFilteredLibraryCount(
        filter = null,
        sortMode = sortMode,
        sortDirection = sortDirection,
      )
    }.orFallback("the library's size", null)

    val isPodcastLibrary = libraryRepository.observeCurrentLibrary(refresh = false)
      .map { it.mediaType == MediaType.Podcast }
      .onStart { emit(false) }
      .orFallback("the current library", false)

    return combine(shelves, layout, itemCount, isPodcastLibrary) { shelfPair, layoutSettings, count, isPodcast ->
      val (continueListening, recentlyAdded) = shelfPair
      val (displayState, sortMode, sortDirection) = layoutSettings
      PreviewLibrary(
        continueListening = continueListening,
        recentlyAdded = recentlyAdded,
        itemCount = count,
        isPodcastLibrary = isPodcast,
        displayState = displayState,
        sortMode = sortMode,
        sortDirection = sortDirection,
      )
    }
      .map { library ->
        if (library.isEmpty) {
          PreviewLibrary.Samples.copy(
            displayState = library.displayState,
            sortMode = library.sortMode,
            sortDirection = library.sortDirection,
          )
        } else {
          library
        }
      }
  }

  private fun observeShelf(shelf: Shelf?): Flow<PreviewShelf> {
    if (shelf == null) return flowOf(PreviewShelf(label = null, items = emptyList()))
    return homeRepository.observeShelf(shelf.id, shelf.type)
      .map { entities ->
        PreviewShelf(
          label = shelf.label,
          items = entities.mapNotNull { it.toPreviewItem() },
        )
      }
      .onStart { emit(PreviewShelf(label = shelf.label, items = emptyList())) }
      .orFallback("the ${shelf.id} shelf", PreviewShelf(label = shelf.label, items = emptyList()))
  }

  /**
   * Stands in [fallback] for a source that fails, so one unreadable source leaves the rest of
   * the preview showing the user's own library rather than samples.
   */
  private fun <T> Flow<T>.orFallback(what: String, fallback: T): Flow<T> = catch { error ->
    bark(TAG, LogPriority.WARN, throwable = error) { "Theme preview couldn't read $what" }
    emit(fallback)
  }

  /**
   * The shelf the server calls [serverId]. Stored shelves carry the user and library after it
   * (`continue-listening_<user>_<library>`), so the server's id is only the start.
   */
  private fun List<Shelf>.serverShelf(serverId: String): Shelf? =
    firstOrNull { it.id == serverId || it.id.startsWith(serverId + "_") }

  private companion object {
    const val TAG = "PreviewContentSource"
  }

  private fun ShelfEntity.toPreviewItem(): PreviewShelfItem? = when (this) {
    is LibraryItem -> PreviewShelfItem(this)
    is ShelfEntity.EpisodeShelfEntry -> PreviewShelfItem(libraryItem, recentEpisode)
    else -> null
  }
}

// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.paged

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.Click
import app.campfire.analytics.events.ContentSelected
import app.campfire.analytics.events.ContentType
import app.campfire.common.compose.util.rememberRetainedCoroutineScope
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.MediaType
import app.campfire.core.model.User
import app.campfire.core.settings.ItemDisplayState
import app.campfire.core.settings.LibraryItemSortModes
import app.campfire.core.settings.PodcastSortModes
import app.campfire.libraries.api.LibraryRepository
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.libraries.api.screen.LibraryScreen
import app.campfire.offline.Downloads
import app.campfire.offline.observeByItem
import app.campfire.podcasts.api.RemoteEpisodeDownloadTracker
import app.campfire.podcasts.api.screen.AddPodcastScreen
import app.campfire.podcasts.api.screen.PodcastDownloadQueueScreen
import app.campfire.settings.api.LibraryViewSettings
import app.campfire.user.api.MediaProgressRepository
import app.campfire.user.api.UserRepository
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.retained.collectAsRetainedState
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.retained.rememberRetainedSaveable
import com.slack.circuit.runtime.Navigator
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal const val INVALID_ITEM_COUNT = -1

@OptIn(ExperimentalCoroutinesApi::class)
@CircuitInject(LibraryScreen::class, UserScope::class)
@Inject
class LibraryPresenter(
  private val screen: LibraryScreen,
  private val navigator: Navigator,
  private val userRepository: UserRepository,
  private val repository: LibraryRepository,
  private val downloads: Downloads,
  private val mediaProgressRepository: MediaProgressRepository,
  private val remoteEpisodeDownloadTracker: RemoteEpisodeDownloadTracker,
  private val libraryViewSettings: LibraryViewSettings,
  private val analytics: Analytics,
) : NonPausablePresenter<LibraryUiState> {

  @Composable
  override fun present(): LibraryUiState {
    // Using a pager requires us to remember the coroutine scope passed the
    // composition of this pager / ui. We should remember it until this screen
    // leaves the back stack
    val scope = rememberRetainedCoroutineScope("library_presenter_scope")

    // Retained so returning from an item starts with the library already known. Starting from null
    // would rekey the pager below, which then pages from the top and loses the list's scroll position
    val currentLibrary by rememberRetained {
      repository.observeCurrentLibrary()
    }.collectAsRetainedState(null)

    var itemFilter by rememberRetainedSaveable {
      mutableStateOf(screen.filter)
    }

    val librarySortMode by remember {
      libraryViewSettings.observeLibrarySortMode()
    }.collectAsState()

    val librarySortDirection by remember {
      libraryViewSettings.observeLibrarySortDirection()
    }.collectAsState()

    val podcastSortMode by remember {
      libraryViewSettings.observePodcastSortMode()
    }.collectAsState()

    val podcastSortDirection by remember {
      libraryViewSettings.observePodcastSortDirection()
    }.collectAsState()

    val currentUser by userRepository.userFlow.collectAsState()

    // Podcast and book libraries sort by different server keys, so the sort depends on the
    // selected library's media type, which is unknown until that library loads (or catches up
    // after a library switch).
    val libraryMediaType = currentLibrary
      ?.takeIf { it.id == currentUser.selectedLibraryId }
      ?.mediaType

    val sort = when (libraryMediaType) {
      MediaType.Podcast -> LibrarySort(podcastSortMode, podcastSortDirection, PodcastSortModes)
      else -> LibrarySort(librarySortMode, librarySortDirection, LibraryItemSortModes)
    }

    val lazyPagingItems = rememberRetained(
      currentUser.id,
      currentUser.selectedLibraryId,
      libraryMediaType,
      sort,
      itemFilter,
    ) {
      if (libraryMediaType == null) {
        // Stays loading rather than paging the library with the other media type's sort
        emptyFlow()
      } else {
        repository.createLibraryItemPager(
          user = currentUser,
          filter = itemFilter,
          sortMode = sort.mode,
          sortDirection = sort.direction,
        ).flow.cachedIn(scope)
      }
    }.collectAsLazyPagingItems()

    val totalItemCount by rememberRetained(libraryMediaType, sort, itemFilter) {
      if (libraryMediaType == null) {
        flowOf(INVALID_ITEM_COUNT)
      } else {
        repository.observeFilteredLibraryCount(
          filter = itemFilter,
          sortMode = sort.mode,
          sortDirection = sort.direction,
        ).map { it ?: INVALID_ITEM_COUNT }
      }
    }.collectAsRetainedState(INVALID_ITEM_COUNT)

    val isPodcastLibrary = libraryMediaType == MediaType.Podcast
    val unfinishedEpisodes by rememberRetained(isPodcastLibrary, sort, itemFilter) {
      if (isPodcastLibrary) {
        repository.observeUnfinishedEpisodes(
          filter = itemFilter,
          sortMode = sort.mode,
          sortDirection = sort.direction,
        )
      } else {
        flowOf(emptyMap())
      }
    }.collectAsRetainedState(emptyMap())

    val itemDisplayState by remember {
      libraryViewSettings.observeLibraryItemDisplayState()
    }.collectAsState()

    val offlineDownloads by remember {
      downloads.observeByItem()
    }.collectAsState(emptyMap())

    // The pages keep the progress they were loaded with, so the cards follow the progress as it
    // changes from here instead, retained so returning to the list starts out knowing it
    val progress by rememberRetained {
      mediaProgressRepository.observeAllProgress()
        .map { all -> all.filter { it.episodeId == null }.associateBy { it.libraryItemId } }
        // Playing writes the progress every few seconds; a card only shows finished, or the percent
        .distinctUntilChanged { old, new -> old.showsAs() == new.showsAs() }
    }.collectAsRetainedState(null)

    val serverDownloads by remember {
      remoteEpisodeDownloadTracker.state
    }.collectAsState()

    val canAddPodcasts by remember {
      derivedStateOf {
        val libraryMediaType = currentLibrary?.mediaType
        libraryMediaType == MediaType.Podcast &&
          (
            currentUser.type == User.Type.Admin ||
              currentUser.type == User.Type.Root
            )
      }
    }

    return LibraryUiState(
      isPodcastLibrary = isPodcastLibrary,
      canAddPodcasts = canAddPodcasts,
      lazyPagingItems = lazyPagingItems,
      totalItemCount = totalItemCount,
      sort = sort,
      filter = itemFilter,
      offlineStates = offlineDownloads,
      progress = progress,
      unfinishedEpisodes = unfinishedEpisodes,
      itemDisplayState = itemDisplayState,
      serverDownloadCount = if (isPodcastLibrary) serverDownloads.values.sumOf { it.size } else 0,
    ) { event ->
      when (event) {
        LibraryUiEvent.ToggleItemDisplayState -> {
          libraryViewSettings.libraryItemDisplayState = when (itemDisplayState) {
            ItemDisplayState.List -> ItemDisplayState.Grid
            ItemDisplayState.Grid -> ItemDisplayState.GridDense
            ItemDisplayState.GridDense -> ItemDisplayState.List
          }.also {
            analytics.send(ActionEvent("item_display", "toggled", it.storageKey))
          }
        }

        is LibraryUiEvent.SortModeSelected -> {
          analytics.send(ActionEvent("sort_mode", "selected", event.mode.storageKey))
          when (libraryMediaType) {
            MediaType.Podcast -> {
              if (podcastSortMode == event.mode) {
                libraryViewSettings.podcastSortDirection = podcastSortDirection.flip()
              }
              libraryViewSettings.podcastSortMode = event.mode
            }
            else -> {
              if (librarySortMode == event.mode) {
                libraryViewSettings.librarySortDirection = librarySortDirection.flip()
              }
              libraryViewSettings.librarySortMode = event.mode
            }
          }
        }

        is LibraryUiEvent.ItemFilterSelected -> {
          analytics.send(ActionEvent("item_filter", "selected"))
          itemFilter = event.filter
        }

        is LibraryUiEvent.ItemClick -> {
          analytics.send(ContentSelected(ContentType.LibraryItem))
          navigator.goTo(LibraryItemScreen(event.libraryItem.id))
        }

        LibraryUiEvent.ServerDownloadsClick -> navigator.goTo(PodcastDownloadQueueScreen)

        LibraryUiEvent.AddPodcastClick -> {
          val libraryId = currentLibrary?.id ?: return@LibraryUiState
          analytics.send(ActionEvent("add_podcast", Click))
          navigator.goTo(AddPodcastScreen(libraryId))
        }
      }
    }
  }
}

/** What the cards show of each item's progress: whether it's finished, and its whole percent */
private fun Map<LibraryItemId, MediaProgress>.showsAs(): Map<LibraryItemId, Pair<Boolean, Int>> =
  mapValues { (_, progress) -> progress.isFinished to (progress.progress * 100).toInt() }

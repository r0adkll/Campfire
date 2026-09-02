// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ContentSelected
import app.campfire.analytics.events.ContentType
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.bookinfo.api.BookInfoRegistry
import app.campfire.bookinfo.api.UpcomingRelease
import app.campfire.common.screens.AuthorDetailScreen
import app.campfire.common.screens.HomeScreen
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.common.screens.UrlScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.ShelfEntity
import app.campfire.home.api.FeedResponse
import app.campfire.home.api.HomeRepository
import app.campfire.home.api.map
import app.campfire.home.api.model.ShelfIds
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.user.api.MediaProgressKey
import app.campfire.user.api.MediaProgressRepository
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.runtime.Navigator
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(HomeScreen::class, UserScope::class)
@Inject
class HomePresenter(
  @Assisted private val navigator: Navigator,
  private val homeRepository: HomeRepository,
  private val mediaProgressRepository: MediaProgressRepository,
  private val offlineDownloadManager: OfflineDownloadManager,
  private val bookInfoRegistry: BookInfoRegistry,
  private val analytics: Analytics,
) : NonPausablePresenter<HomeUiState> {

  @Suppress("UNCHECKED_CAST")
  @OptIn(ExperimentalCoroutinesApi::class)
  @Composable
  override fun present(): HomeUiState {
    // Observe just the shelf information. We will use this to compose the remaining elements
    val domainFeed by remember {
      homeRepository.observeHomeFeed()
    }.collectAsState(FeedResponse.Loading)

    // We want to make sure we are constantly and consistently observing the items for each shelf
    // so we are not constantly restarting the observations unless the root shelves themselves change
    val shelfEntities by remember {
      snapshotFlow { domainFeed.dataOrNull }
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { shelves ->
          val shelfFlows = shelves.map { shelf ->
            homeRepository.observeShelf(shelf.id, shelf.type)
              .map { LoadState.Loaded(it) as LoadState<List<ShelfEntity>> }
              .onStart { emit(LoadState.Loading as LoadState<List<ShelfEntity>>) }
              .catch { emit(LoadState.Error as LoadState<List<ShelfEntity>>) }
              .map { entityLoadState ->
                shelf.id to entityLoadState
              }
          }

          combine(
            flows = shelfFlows,
            transform = { shelfFlows ->
              persistentMapOf(*shelfFlows)
            },
          )
        }
    }.collectAsState(persistentMapOf())

    val upcomingReleases by remember {
      bookInfoRegistry.observeCachedUpcoming()
    }.collectAsState(emptyList())

    // Now combine both the shelves and entities into the final set of UiShelf to render
    // in the UI, weaving the locally sourced upcoming shelf into the server's feed.
    val feed by remember {
      derivedStateOf {
        domainFeed.map { shelves ->
          val uiShelves = shelves.map { shelf ->
            UiShelf(
              shelf,
              shelfEntities[shelf.id]
                ?: LoadState.Loading as LoadState<List<ShelfEntity>>,
            )
          }
          insertUpcomingShelf(uiShelves, upcomingReleases).toPersistentList()
        }
      }
    }

    val userMediaProgress by remember {
      mediaProgressRepository.observeAllProgress()
        .map { allProgress ->
          allProgress
            .associateBy { MediaProgressKey(it) }
            .toPersistentMap()
        }
    }.collectAsState(persistentMapOf())

    val offlineDownloads by remember {
      snapshotFlow { shelfEntities.values }
        .map { responses ->
          responses
            .mapNotNull { it.dataOrNull }
            .flatten()
            .filterIsInstance<LibraryItem>()
        }
        .flatMapLatest { libraryItems ->
          offlineDownloadManager.observeForItems(libraryItems)
            .map { it.toPersistentMap() }
        }
    }.collectAsState(persistentMapOf())

    return HomeUiState(
      homeFeed = feed,
      offlineStates = offlineDownloads,
      progressStates = userMediaProgress,
    ) { event ->
      when (event) {
        is HomeUiEvent.OpenLibraryItem -> {
          analytics.send(ContentSelected(ContentType.LibraryItem))
          navigator.goTo(LibraryItemScreen(event.item.id, event.sharedTransitionKey))
        }
        is HomeUiEvent.OpenLibraryItemWithEpisode -> {
          analytics.send(ContentSelected(ContentType.LibraryItem))
          navigator.goTo(LibraryItemScreen(event.item.id, event.sharedTransitionKey, event.episodeId))
        }
        is HomeUiEvent.OpenAuthor -> {
          analytics.send(ContentSelected(ContentType.Author))
          navigator.goTo(AuthorDetailScreen(event.author.id, event.author.name))
        }
        is HomeUiEvent.OpenSeries -> {
          analytics.send(ContentSelected(ContentType.Series))
          navigator.goTo(SeriesDetailScreen(event.series.id, event.series.name))
        }
        is HomeUiEvent.OpenUpcomingBook -> navigator.goTo(UrlScreen(event.url))
      }
    }
  }
}

/**
 * Weaves the cached upcoming shelf into the server's feed — after
 * Recently Added when present (between it and Recent Series in the default
 * feed order), at the end otherwise. Nothing is inserted with nothing cached.
 * The label stays empty here; the UI resolves it from resources by shelf id.
 */
private fun insertUpcomingShelf(
  shelves: List<UiShelf<ShelfEntity>>,
  upcoming: List<UpcomingRelease>,
): List<UiShelf<ShelfEntity>> {
  if (upcoming.isEmpty()) return shelves

  val upcomingShelf = UiShelf<ShelfEntity>(
    id = ShelfIds.UpcomingReleases,
    label = "",
    total = upcoming.size,
    entities = LoadState.Loaded(
      upcoming.map { release ->
        ShelfEntity.UpcomingBookShelfEntry(
          id = "${release.providerId.key}:${release.entry.providerBookId}",
          title = release.entry.title,
          seriesName = release.seriesName,
          releaseDate = release.entry.releaseDate,
          coverUrl = release.entry.coverUrl,
          providerUrl = release.entry.providerUrl,
        )
      },
    ),
  )

  val anchor = shelves.indexOfFirst { it.id == ShelfIds.RecentlyAdded }
  return if (anchor >= 0) {
    shelves.toMutableList().apply { add(anchor + 1, upcomingShelf) }
  } else {
    shelves + upcomingShelf
  }
}

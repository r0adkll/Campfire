package app.campfire.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ContentSelected
import app.campfire.analytics.events.ContentType
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.common.screens.AuthorDetailScreen
import app.campfire.common.screens.HomeScreen
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.ShelfEntity
import app.campfire.home.api.FeedResponse
import app.campfire.home.api.HomeRepository
import app.campfire.home.api.flatMapLatestSuccess
import app.campfire.libraries.api.screen.LibraryItemScreen
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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
  private val offlineDownloadManager: OfflineDownloadManager,
  private val analytics: Analytics,
) : Presenter<HomeUiState> {

  @OptIn(ExperimentalCoroutinesApi::class)
  @Composable
  override fun present(): HomeUiState {
    // Observe JUST the [Shelf] themselves and not the containing
    // entities themselves. Those will be observed separately
    @Suppress("UNCHECKED_CAST")
    val feed by remember {
      homeRepository.observeHomeFeed()
        .flatMapLatestSuccess { shelves ->
          val shelfFlows = shelves.map { shelf ->
            homeRepository.observeShelf(shelf.id, shelf.type)
              .map { LoadState.Loaded(it) as LoadState<List<ShelfEntity>> }
              .onStart { emit(LoadState.Loading as LoadState<List<ShelfEntity>>) }
              .catch { emit(LoadState.Error as LoadState<List<ShelfEntity>>) }
              .map { entityLoadState ->
                UiShelf(shelf, entityLoadState)
              }
          }

          combine(
            flows = shelfFlows,
            transform = { shelfFlows ->
              shelfFlows.toPersistentList()
            },
          )
        }
    }.collectAsState(FeedResponse.Loading)

    val userMediaProgress by remember {
      snapshotFlow { feed.dataOrNull }
        .filterNotNull()
        .flatMapLatest { shelves ->
          val libraryItemIds = shelves
            .flatMap { it.entities.dataOrNull ?: emptyList() }
            .filterIsInstance<LibraryItem>()
            .map { it.id }

          homeRepository.observeMediaProgress(libraryItemIds)
            .map { it.toPersistentMap() }
        }
    }.collectAsState(persistentMapOf())

    val offlineDownloads by remember {
      snapshotFlow { feed.dataOrNull }
        .filterNotNull()
        .flatMapLatest { items ->
          val libraryItems = items
            .flatMap { it.entities.dataOrNull ?: emptyList() }
            .filterIsInstance<LibraryItem>()
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
        is HomeUiEvent.OpenAuthor -> {
          analytics.send(ContentSelected(ContentType.Author))
          navigator.goTo(AuthorDetailScreen(event.author.id, event.author.name))
        }
        is HomeUiEvent.OpenSeries -> {
          analytics.send(ContentSelected(ContentType.Series))
          navigator.goTo(SeriesDetailScreen(event.series.id, event.series.name))
        }
      }
    }
  }
}

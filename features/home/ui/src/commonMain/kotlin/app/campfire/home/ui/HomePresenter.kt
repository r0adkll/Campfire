package app.campfire.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.common.screens.AuthorDetailScreen
import app.campfire.common.screens.HomeScreen
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItem
import app.campfire.home.api.HomeRepository
import app.campfire.libraries.api.screen.LibraryItemScreen
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(HomeScreen::class, UserScope::class)
@Inject
class HomePresenter(
  @Assisted private val navigator: Navigator,
  private val homeRepository: HomeRepository,
  private val offlineDownloadManager: OfflineDownloadManager,
) : Presenter<HomeUiState> {

  @OptIn(ExperimentalCoroutinesApi::class)
  @Composable
  override fun present(): HomeUiState {
    val feed by remember {
      homeRepository.observeHomeFeed()
        .map { LoadState.Loaded(it) }
        .catch { LoadState.Error }
    }.collectAsState(LoadState.Loading)

    val offlineDownloads by remember {
      snapshotFlow { feed.dataOrNull }
        .filterNotNull()
        .flatMapLatest { items ->
          val libraryItems = items
            .flatMap { it.entities }
            .filterIsInstance<LibraryItem>()
          offlineDownloadManager.observeForItems(libraryItems)
        }
    }.collectAsState(emptyMap())

    return HomeUiState(
      homeFeed = feed,
      offlineStates = offlineDownloads,
    ) { event ->
      when (event) {
        is HomeUiEvent.OpenLibraryItem -> navigator.goTo(LibraryItemScreen(event.item.id))
        is HomeUiEvent.OpenAuthor -> navigator.goTo(AuthorDetailScreen(event.author.id, event.author.name))
        is HomeUiEvent.OpenSeries -> navigator.goTo(SeriesDetailScreen(event.series.id, event.series.name))
      }
    }
  }
}

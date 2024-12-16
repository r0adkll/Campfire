package app.campfire.libraries.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import app.campfire.audioplayer.PlaybackController
import app.campfire.common.screens.LibraryItemScreen
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.coroutines.map
import app.campfire.core.di.UserScope
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.series.api.SeriesRepository
import app.campfire.sessions.api.SessionsRepository
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(LibraryItemScreen::class, UserScope::class)
@Inject
class LibraryItemPresenter(
  @Assisted private val screen: LibraryItemScreen,
  @Assisted private val navigator: Navigator,
  private val repository: LibraryItemRepository,
  private val seriesRepository: SeriesRepository,
  private val sessionsRepository: SessionsRepository,
  private val playbackController: PlaybackController,
) : Presenter<LibraryItemUiState> {

  @OptIn(ExperimentalCoroutinesApi::class)
  @Composable
  override fun present(): LibraryItemUiState {
    val currentSession by remember {
      sessionsRepository.observeCurrentSession()
        .filterNotNull()
        .filter { it.libraryItem.id == screen.libraryItemId }
        .map { SessionUiState.Current(it) }
    }.collectAsState(SessionUiState.None)

    val libraryItemContentState by remember {
      repository.observeLibraryItem(screen.libraryItemId)
        .map { LoadState.Loaded(it) }
        .catch { LoadState.Error }
    }.collectAsState(LoadState.Loading)

    val seriesContentState by remember {
      snapshotFlow {
        libraryItemContentState.dataOrNull
          ?.media?.metadata?.seriesSequence
      }
        .filterNotNull()
        .flatMapLatest { seriesSequence ->
          seriesRepository.observeSeriesLibraryItems(seriesSequence.id)
            .map { LoadState.Loaded(it) }
            .catch { LoadState.Error }
        }
    }.collectAsState(LoadState.Loading)

    return LibraryItemUiState(
      sessionUiState = currentSession,
      libraryItemContentState = libraryItemContentState,
      seriesContentState = seriesContentState,
    ) { event ->
      when (event) {
        LibraryItemUiEvent.OnBack -> navigator.pop()
        is LibraryItemUiEvent.PlayClick -> {
          playbackController.startSession(event.item.id)
        }
        is LibraryItemUiEvent.SeriesClick -> {
          val series = event.item.media.metadata.seriesSequence ?: return@LibraryItemUiState
          navigator.goTo(SeriesDetailScreen(series.id, series.name))
        }
      }
    }
  }
}

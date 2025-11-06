package app.campfire.libraries.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.Click
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.audioplayer.PlaybackController
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.common.screens.AuthorDetailScreen
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.MediaProgress
import app.campfire.libraries.api.LibraryItemFilter
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.libraries.api.screen.LibraryScreen
import app.campfire.series.api.SeriesRepository
import app.campfire.sessions.api.SessionsRepository
import app.campfire.settings.api.CampfireSettings
import app.campfire.user.api.MediaProgressRepository
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
  private val mediaProgressRepository: MediaProgressRepository,
  private val playbackController: PlaybackController,
  private val audioPlayerHolder: AudioPlayerHolder,
  private val offlineDownloadManager: OfflineDownloadManager,
  private val settings: CampfireSettings,
  private val analytics: Analytics,
) : Presenter<LibraryItemUiState> {

  @Suppress("UNCHECKED_CAST")
  @OptIn(ExperimentalCoroutinesApi::class)
  @Composable
  override fun present(): LibraryItemUiState {
    val scope = rememberCoroutineScope()

    val currentSession by remember {
      sessionsRepository.observeCurrentSession()
        .filterNotNull()
        .filter { it.libraryItem.id == screen.libraryItemId }
        .map { SessionUiState.Current(it) }
    }.collectAsState(SessionUiState.None)

    val libraryItemContentState by remember {
      repository.observeLibraryItem(screen.libraryItemId)
        .map { LoadState.Loaded(it) as LoadState<LibraryItem> }
        .catch { emit(LoadState.Error as LoadState<LibraryItem>) }
    }.collectAsState(LoadState.Loading)

    val mediaProgressState by remember {
      mediaProgressRepository.observeProgress(screen.libraryItemId)
        .map { LoadState.Loaded(it) as LoadState<MediaProgress?> }
        .catch { emit(LoadState.Error as LoadState<MediaProgress?>) }
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

    val offlineDownloadState by remember {
      snapshotFlow { libraryItemContentState.dataOrNull }
        .filterNotNull()
        .flatMapLatest { libraryItem ->
          offlineDownloadManager.observeForItem(libraryItem)
        }
    }.collectAsState(null)

    val showConfirmDownloadDialog by remember {
      settings.observeShowConfirmDownload()
    }.collectAsState(true)

    val showTimeInBook by remember {
      settings.observeShowTimeInBook()
    }.collectAsState(true)

    return LibraryItemUiState(
      sessionUiState = currentSession,
      libraryItemContentState = libraryItemContentState,
      offlineDownloadState = offlineDownloadState,
      seriesContentState = seriesContentState,
      mediaProgressState = mediaProgressState,
      showConfirmDownloadDialog = showConfirmDownloadDialog,
      showTimeInBook = showTimeInBook,
    ) { event ->
      when (event) {
        LibraryItemUiEvent.OnBack -> navigator.pop()
        is LibraryItemUiEvent.PlayClick -> {
          analytics.send(ActionEvent("add_to_collection", Click))
          playbackController.startSession(event.item.id)
        }

        is LibraryItemUiEvent.AuthorClick -> {
          analytics.send(ActionEvent("author", Click))
          event.item.media.metadata.authors.firstOrNull()?.let { author ->
            navigator.goTo(AuthorDetailScreen(author.id, author.name))
          }
        }

        is LibraryItemUiEvent.NarratorClick -> {
          analytics.send(ActionEvent("narrator", Click))
          event.item.media.metadata.narratorName?.let { narrator ->
            navigator.goTo(LibraryScreen(LibraryItemFilter.Narrators(narrator)))
          }
        }

        is LibraryItemUiEvent.SeriesClick -> {
          analytics.send(ActionEvent("series", Click))
          val series = event.item.media.metadata.seriesSequence ?: return@LibraryItemUiState
          navigator.goTo(SeriesDetailScreen(series.id, series.name))
        }

        is LibraryItemUiEvent.DiscardProgress -> {
          analytics.send(ActionEvent("discard_progress", Click))
          playbackController.stopSession(event.item.id)
          scope.launch {
            sessionsRepository.deleteSession(event.item.id)
            mediaProgressRepository.deleteProgress(event.item.id)
          }
        }

        is LibraryItemUiEvent.MarkFinished -> {
          analytics.send(ActionEvent("mark_finished", Click))
          playbackController.stopSession(event.item.id)
          scope.launch {
            sessionsRepository.deleteSession(event.item.id)
            mediaProgressRepository.markFinished(event.item.id)
          }
        }

        is LibraryItemUiEvent.MarkNotFinished -> {
          analytics.send(ActionEvent("mark_not_finished", Click))
          scope.launch {
            mediaProgressRepository.markNotFinished(event.item.id)
          }
        }

        is LibraryItemUiEvent.ChapterClick -> {
          analytics.send(ActionEvent("chapter", Click))
          val session = (currentSession as? SessionUiState.Current)?.session
          val currentPlayer = audioPlayerHolder.currentPlayer.value
          if (event.item.id == session?.libraryItem?.id && currentPlayer != null) {
            // Just seek to the chapter id
            audioPlayerHolder.currentPlayer.value?.seekTo(event.chapter.id)
              ?: throw IllegalStateException("Current session doesn't have a player")
          } else {
            // Start a new session for the item at the given chapter
            playbackController.startSession(event.item.id, true, event.chapter.id)
          }
        }

        is LibraryItemUiEvent.DownloadClick -> {
          analytics.send(ActionEvent("download", Click))
          settings.showConfirmDownload = !event.doNotShowAgain

          libraryItemContentState.dataOrNull?.let {
            offlineDownloadManager.download(it)
          }
        }

        LibraryItemUiEvent.RemoveDownloadClick -> {
          analytics.send(ActionEvent("delete_download", Click))

          libraryItemContentState.dataOrNull?.let {
            offlineDownloadManager.delete(it)
          }
        }

        LibraryItemUiEvent.StopDownloadClick -> {
          analytics.send(ActionEvent("stop_download", Click))
          libraryItemContentState.dataOrNull?.let {
            offlineDownloadManager.stop(it)
          }
        }

        is LibraryItemUiEvent.TimeInBookChange -> {
          analytics.send(ActionEvent("time_in_book", Click))
          settings.showTimeInBook = event.enabled
        }
      }
    }
  }
}

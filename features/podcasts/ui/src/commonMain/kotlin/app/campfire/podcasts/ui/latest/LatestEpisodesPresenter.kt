package app.campfire.podcasts.ui.latest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import app.campfire.audioplayer.PlaybackController
import app.campfire.audioplayer.history.PlaybackHistoryRepository
import app.campfire.common.compose.util.rememberRetainedCoroutineScope
import app.campfire.core.di.UserScope
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.playlists.api.dialog.AddToPlaylistDialog
import app.campfire.podcasts.api.PodcastsRepository
import app.campfire.podcasts.api.screen.LatestEpisodesScreen
import app.campfire.sessions.api.SessionsRepository
import app.campfire.user.api.MediaProgressKey
import app.campfire.user.api.MediaProgressRepository
import app.campfire.user.api.UserRepository
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@CircuitInject(LatestEpisodesScreen::class, UserScope::class)
@Inject
class LatestEpisodesPresenter(
  @Assisted private val navigator: Navigator,
  private val userRepository: UserRepository,
  private val podcastsRepository: PodcastsRepository,
  private val sessionsRepository: SessionsRepository,
  private val mediaProgressRepository: MediaProgressRepository,
  private val playbackHistoryRepository: PlaybackHistoryRepository,
  private val playbackController: PlaybackController,
  private val addToPlaylistDialog: AddToPlaylistDialog,
) : NonPausablePresenter<LatestEpisodesUiState> {

  @Composable
  override fun present(): LatestEpisodesUiState {
    val scope = rememberRetainedCoroutineScope()

    val currentSession by remember {
      sessionsRepository.observeCurrentSession()
    }.collectAsState(null)

    val currentUser by userRepository.observeStatefulCurrentUser().collectAsState()

    val pagingItems = rememberRetained(currentUser.id, currentUser.selectedLibraryId) {
      podcastsRepository.createLatestEpisodesPager(currentUser).flow.cachedIn(scope)
    }.collectAsLazyPagingItems()

    val mediaProgress by remember {
      mediaProgressRepository.observeAllProgress()
        .map {
          it.associateBy { progress -> MediaProgressKey(progress) }
        }
    }.collectAsState(emptyMap())

    return LatestEpisodesUiState(
      currentSession = currentSession,
      episodes = pagingItems,
      mediaProgress = mediaProgress,
      addToPlaylistDialog = addToPlaylistDialog,
      eventSink = { event ->
        when (event) {
          is LatestEpisodesUiEvent.PlayEpisode -> {
            playbackController.startSession(
              itemId = event.libraryItemId,
              episodeId = event.episodeId,
            )
          }
          is LatestEpisodesUiEvent.OpenPodcast -> {
            navigator.goTo(
              LibraryItemScreen(
                libraryItemId = event.episode.episode.libraryItemId,
                episodeId = event.episode.episode.id,
              ),
            )
          }
          is LatestEpisodesUiEvent.MarkFinished -> {
            val episode = event.episode.episode

            // Only stop playback if this episode is the one currently playing.
            if (
              currentSession?.libraryItem?.id == episode.libraryItemId &&
              currentSession?.episodeId == episode.id
            ) {
              playbackController.stopSession(
                itemId = episode.libraryItemId,
                episodeId = episode.id,
              )
            }

            scope.launch {
              sessionsRepository.markDeleted(episode.libraryItemId, episode.id)
              mediaProgressRepository.markFinished(episode.libraryItemId, episode.id)
              playbackHistoryRepository.clear(episode.libraryItemId, episode.id)
            }
          }
          is LatestEpisodesUiEvent.MarkNotFinished -> {
            val episode = event.episode.episode
            scope.launch {
              mediaProgressRepository.markNotFinished(episode.libraryItemId, episode.id)
            }
          }
        }
      },
    )
  }
}

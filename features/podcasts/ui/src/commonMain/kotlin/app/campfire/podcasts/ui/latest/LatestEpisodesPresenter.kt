// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.ui.latest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import app.campfire.audioplayer.PlaybackController
import app.campfire.audioplayer.history.PlaybackHistoryRepository
import app.campfire.common.compose.layout.LocalSnackBarHost
import app.campfire.common.compose.message.showAddedToQueue
import app.campfire.common.compose.message.showEpisodeDeleted
import app.campfire.common.compose.message.showEpisodeNotDeleted
import app.campfire.common.compose.message.showRemovedFromQueue
import app.campfire.common.compose.util.rememberRetainedCoroutineScope
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.bark
import app.campfire.libraries.api.ItemChangeNotAllowedException
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.playlists.api.dialog.AddToPlaylistDialog
import app.campfire.podcasts.api.PodcastsRepository
import app.campfire.podcasts.api.RemoteEpisodeDownloadTracker
import app.campfire.podcasts.api.screen.LatestEpisodesScreen
import app.campfire.podcasts.api.screen.PodcastDownloadQueueScreen
import app.campfire.sessions.api.SessionQueue
import app.campfire.sessions.api.SessionsRepository
import app.campfire.user.api.MediaProgressKey
import app.campfire.user.api.MediaProgressRepository
import app.campfire.user.api.UserRepository
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@CircuitInject(LatestEpisodesScreen::class, UserScope::class)
@Inject
class LatestEpisodesPresenter(
  private val navigator: Navigator,
  private val userRepository: UserRepository,
  private val podcastsRepository: PodcastsRepository,
  private val sessionsRepository: SessionsRepository,
  private val mediaProgressRepository: MediaProgressRepository,
  private val playbackHistoryRepository: PlaybackHistoryRepository,
  private val playbackController: PlaybackController,
  private val sessionQueue: SessionQueue,
  private val addToPlaylistDialog: AddToPlaylistDialog,
  private val remoteEpisodeDownloadTracker: RemoteEpisodeDownloadTracker,
  @ForScope(UserScope::class) private val coroutineScopeHolder: CoroutineScopeHolder,
) : NonPausablePresenter<LatestEpisodesUiState> {

  @Composable
  override fun present(): LatestEpisodesUiState {
    val scope = rememberRetainedCoroutineScope("last_episodes_presenter")

    // Only which item is playing: the session itself changes with every saved position
    val playingItem by remember {
      sessionsRepository.observePlayingItem()
    }.collectAsState(null)

    val currentUser by userRepository.userFlow.collectAsState()

    val pagingItems = rememberRetained(
      currentUser.id,
      currentUser.selectedLibraryId,
    ) {
      podcastsRepository.createLatestEpisodesPager(currentUser)
        .flow.cachedIn(scope)
    }.collectAsLazyPagingItems()

    // Only episodes' progress, so a book playing elsewhere doesn't redraw the list each time it
    // saves its position
    val mediaProgress by remember {
      mediaProgressRepository.observeAllProgress()
        .map { all ->
          all.filter { it.episodeId != null }
            .associateBy { progress -> MediaProgressKey(progress) }
        }
        .distinctUntilChanged()
    }.collectAsState(emptyMap())

    val queuedEpisodeIds by remember {
      sessionQueue.observeAll()
        .map { entries -> entries.mapNotNullTo(HashSet()) { it.episodeId } }
        .distinctUntilChanged()
    }.collectAsState(emptySet())

    // The app's own snackbar, so queueing tells what it did without passing it through the state
    val snackbarHost = LocalSnackBarHost.current

    val serverDownloads by remember {
      remoteEpisodeDownloadTracker.state
    }.collectAsState()

    return LatestEpisodesUiState(
      playingItem = playingItem,
      episodes = pagingItems,
      mediaProgress = mediaProgress,
      queuedEpisodeIds = queuedEpisodeIds,
      addToPlaylistDialog = addToPlaylistDialog,
      serverDownloadCount = serverDownloads.values.sumOf { it.size },
      canDeleteEpisodes = currentUser.canDeleteItems,
      eventSink = { event ->
        when (event) {
          LatestEpisodesUiEvent.ServerDownloadsClick -> navigator.goTo(PodcastDownloadQueueScreen)

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
              playingItem?.matches(episode.libraryItemId, episode.id) == true
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

          is LatestEpisodesUiEvent.AddToQueue -> {
            val episode = event.episode.episode
            scope.launch {
              sessionQueue.add(episode.libraryItemId, episode.id)
              if (snackbarHost.showAddedToQueue()) {
                sessionQueue.remove(episode.libraryItemId, episode.id)
              }
            }
          }

          is LatestEpisodesUiEvent.DeleteEpisode -> {
            if (!currentUser.canDeleteItems) return@LatestEpisodesUiState
            val episode = event.episode.episode
            // Runs on past this screen, which the deleted episode may have been the last reason to be on
            coroutineScopeHolder.get().launch {
              podcastsRepository.deleteEpisode(episode.libraryItemId, episode.id, event.hardDelete)
                .onSuccess { snackbarHost.showEpisodeDeleted(episode.title, event.hardDelete) }
                .onFailure { error ->
                  bark(throwable = error) { "Failed to delete episode ${episode.id}" }
                  snackbarHost.showEpisodeNotDeleted(notAllowed = error is ItemChangeNotAllowedException)
                }
            }
          }

          is LatestEpisodesUiEvent.RemoveFromQueue -> {
            val episode = event.episode.episode
            scope.launch {
              val index = sessionQueue.remove(episode.libraryItemId, episode.id) ?: return@launch
              if (snackbarHost.showRemovedFromQueue()) {
                sessionQueue.insert(episode.libraryItemId, episode.id, index)
              }
            }
          }
        }
      },
    )
  }
}

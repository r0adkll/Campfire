// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.podcast.episode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.Click
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.audioplayer.PlaybackController
import app.campfire.audioplayer.history.PlaybackHistoryRepository
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.PodcastEpisode
import app.campfire.libraries.ui.detail.SessionUiState
import app.campfire.playlists.api.dialog.AddToPlaylistDialog
import app.campfire.sessions.api.SessionQueue
import app.campfire.sessions.api.SessionsRepository
import app.campfire.sessions.api.observeContains
import app.campfire.settings.api.CampfireSettings
import app.campfire.user.api.MediaProgressRepository
import com.slack.circuit.overlay.OverlayNavigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias PodcastEpisodePresenterFactory =
  (LibraryItem, PodcastEpisode, OverlayNavigator<Unit>) -> PodcastEpisodePresenter

@OptIn(ExperimentalCoroutinesApi::class)
@Inject
class PodcastEpisodePresenter(
  @Assisted private val libraryItem: LibraryItem,
  @Assisted private val episode: PodcastEpisode,
  @Assisted private val navigator: OverlayNavigator<Unit>,
  private val analytics: Analytics,
  private val playbackController: PlaybackController,
  private val sessionsRepository: SessionsRepository,
  private val sessionQueue: SessionQueue,
  private val mediaProgressRepository: MediaProgressRepository,
  private val playbackHistoryRepository: PlaybackHistoryRepository,
  private val audioPlayerHolder: AudioPlayerHolder,
  private val offlineDownloadManager: OfflineDownloadManager,
  private val settings: CampfireSettings,
  private val addToPlaylistDialog: AddToPlaylistDialog,
) : Presenter<PodcastEpisodeUiState> {

  @Composable
  override fun present(): PodcastEpisodeUiState {
    val scope = rememberCoroutineScope()

    val currentSession by remember {
      sessionsRepository.observeCurrentSession()
    }.collectAsState(null)

    val episodeSession by remember {
      derivedStateOf {
        currentSession
          ?.takeIf {
            it.libraryItem.id == episode.libraryItemId &&
              it.episodeId == episode.id
          }
          ?.let { SessionUiState.Current(it) }
          ?: SessionUiState.None
      }
    }

    val progress by remember {
      mediaProgressRepository.observeProgress(
        libraryItemId = episode.libraryItemId,
        episodeId = episode.id,
      )
    }.collectAsState(null)

    val isPlaying by remember {
      audioPlayerHolder.currentPlayer
        .flatMapLatest {
          if (
            it?.preparedSession?.libraryItem?.id == episode.libraryItemId &&
            it.preparedSession?.episodeId == episode.id
          ) {
            it.state
          } else {
            flowOf(AudioPlayer.State.Disabled)
          }
        }
        .mapLatest { it == AudioPlayer.State.Playing }
        .distinctUntilChanged()
    }.collectAsState(false)

    val playbackSpeed by remember {
      audioPlayerHolder.currentPlayer
        .flatMapLatest {
          if (
            it?.preparedSession?.libraryItem?.id == episode.libraryItemId &&
            it.preparedSession?.episodeId == episode.id
          ) {
            it.playbackSpeed
          } else {
            flowOf(1f)
          }
        }
        .distinctUntilChanged()
    }.collectAsState(1f)

    val isQueued by remember {
      sessionQueue.observeContains(episode.libraryItemId, episode.id)
    }.collectAsState(false)

    val offlineDownload by remember(libraryItem, episode) {
      offlineDownloadManager.observeForEpisode(libraryItem, episode)
    }.collectAsState(null)

    val showConfirmDownloadDialog by remember {
      settings.observeShowConfirmDownload()
    }.collectAsState()

    return PodcastEpisodeUiState(
      libraryItem = libraryItem,
      episode = episode,
      progress = progress,
      playbackSpeed = playbackSpeed,
      isPlaying = isPlaying,
      isQueued = isQueued,
      hasSession = currentSession != null,
      sessionState = episodeSession,
      offlineDownload = offlineDownload,
      showConfirmDownloadDialog = showConfirmDownloadDialog,
      addToPlaylistDialog = addToPlaylistDialog,
    ) { event ->
      when (event) {
        PodcastEpisodeUiEvent.PlayClick -> {
          analytics.send(ActionEvent("play_item", Click))
          playbackController.startSession(
            itemId = episode.libraryItemId,
            episodeId = episode.id,
          )
          navigator.finish(Unit)
        }

        PodcastEpisodeUiEvent.MarkFinished -> {
          analytics.send(ActionEvent("mark_finished", Click))

          // Only stop session if this episode is the current playing episode.
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

        PodcastEpisodeUiEvent.MarkNotFinished -> {
          analytics.send(ActionEvent("mark_not_finished", Click))
          scope.launch {
            mediaProgressRepository.markNotFinished(episode.libraryItemId, episode.id)
          }
        }

        is PodcastEpisodeUiEvent.Seek -> {
          analytics.send(ActionEvent("seek_timestamp", Click))
          val player = audioPlayerHolder.currentPlayer.value
          if (
            player != null &&
            player.preparedSession?.libraryItem?.id == episode.libraryItemId &&
            player.preparedSession?.episodeId == episode.id
          ) {
            player.seekTo(event.position)
          } else {
            scope.launch {
              playbackController.startSession(
                itemId = episode.libraryItemId,
                episodeId = episode.id,
                playImmediately = true,
              )
              audioPlayerHolder.currentPlayer
                .filterNotNull()
                .first {
                  it.preparedSession?.libraryItem?.id == episode.libraryItemId &&
                    it.preparedSession?.episodeId == episode.id
                }
                .seekTo(event.position)
              navigator.finish(Unit)
            }
          }
        }

        PodcastEpisodeUiEvent.DiscardProgress -> {
          analytics.send(ActionEvent("discard_progress", Click))

          // Only stop session if this episode is the current playing episode.
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
            mediaProgressRepository.deleteProgress(episode.libraryItemId, episode.id)
            playbackHistoryRepository.clear(episode.libraryItemId, episode.id)
          }
        }

        PodcastEpisodeUiEvent.AddToQueue -> {
          analytics.send(ActionEvent("add_to_queue", Click))
          scope.launch {
            sessionQueue.add(libraryItem, episode)
          }
        }

        PodcastEpisodeUiEvent.RemoveFromQueue -> {
          analytics.send(ActionEvent("remove_from_queue", Click))
          scope.launch {
            sessionQueue.remove(episode.libraryItemId, episode.id)
          }
        }

        is PodcastEpisodeUiEvent.DownloadClick -> {
          analytics.send(ActionEvent("download_episode", Click))
          settings.showConfirmDownload = !event.doNotShowAgain
          offlineDownloadManager.downloadEpisode(libraryItem, episode)
        }

        PodcastEpisodeUiEvent.RemoveDownloadClick -> {
          analytics.send(ActionEvent("delete_episode_download", Click))
          offlineDownloadManager.deleteEpisode(libraryItem, episode)
        }

        PodcastEpisodeUiEvent.StopDownloadClick -> {
          analytics.send(ActionEvent("stop_episode_download", Click))
          offlineDownloadManager.stopEpisode(libraryItem, episode)
        }
      }
    }
  }
}

// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.playlists.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.ContentSelected
import app.campfire.analytics.events.ContentType
import app.campfire.audioplayer.PlaybackController
import app.campfire.audioplayer.history.PlaybackHistoryRepository
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.model.Playlist
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.playlists.api.PlaylistsRepository
import app.campfire.playlists.api.screen.PlaylistDetailScreen
import app.campfire.sessions.api.SessionQueue
import app.campfire.sessions.api.SessionsRepository
import app.campfire.settings.api.LibraryViewSettings
import app.campfire.user.api.MediaProgressKey
import app.campfire.user.api.MediaProgressRepository
import app.campfire.user.api.UserRepository
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@CircuitInject(PlaylistDetailScreen::class, UserScope::class)
@Inject
class PlaylistDetailPresenter(
  private val screen: PlaylistDetailScreen,
  private val navigator: Navigator,
  private val analytics: Analytics,
  private val playlistsRepository: PlaylistsRepository,
  private val playbackController: PlaybackController,
  private val sessionsRepository: SessionsRepository,
  private val sessionQueue: SessionQueue,
  private val downloadManager: OfflineDownloadManager,
  private val userRepository: UserRepository,
  private val libraryViewSettings: LibraryViewSettings,
  private val mediaProgressRepository: MediaProgressRepository,
  private val playbackHistoryRepository: PlaybackHistoryRepository,
  @ForScope(UserScope::class) private val userScopeHolder: CoroutineScopeHolder,
) : Presenter<PlaylistDetailUiState> {

  @Composable
  override fun present(): PlaylistDetailUiState {
    val scope = rememberCoroutineScope()

    val session by remember {
      sessionsRepository.observeCurrentSession()
    }.collectAsState(null)

    val offlineStates by remember {
      downloadManager.observeAll()
        .map { it.associateBy { item -> item.libraryItemId } }
    }.collectAsState(emptyMap())

    val progressStates by remember {
      mediaProgressRepository.observeAllProgress()
        .map { allProgress -> allProgress.associateBy { MediaProgressKey(it) } }
    }.collectAsState(emptyMap())

    val playlistContentState by remember {
      playlistsRepository.observePlaylist(screen.playlistId, screen.isCreatedId)
        .map { LoadState.Loaded(it) }
        .catch<LoadState<out Playlist>> { emit(LoadState.Error) }
    }.collectAsState(LoadState.Loading)

    val playlistId by remember {
      derivedStateOf {
        playlistContentState.dataOrNull?.id
          ?: screen.playlistId
      }
    }

    val playlistName by remember {
      derivedStateOf {
        playlistContentState.dataOrNull?.name
          ?: screen.playlistName
          ?: ""
      }
    }

    val playlistDescription by remember {
      derivedStateOf {
        @Suppress("SimpleRedundantLet")
        playlistContentState.dataOrNull?.let {
          it.description
        } ?: screen.playlistDescription
      }
    }

    val playlistItemsState by remember(playlistId) {
      playlistsRepository.observePlaylistItems(playlistId)
        .map { LoadState.Loaded(it) }
        .catch<LoadState<out List<Playlist.Item.Expanded>>> { emit(LoadState.Error) }
    }.collectAsState(LoadState.Loading)

    // A local cache of playlist items that we can re-order and then dispatch when finished
    val playlistItems = remember(playlistItemsState.dataOrNull) {
      playlistItemsState.dataOrNull
        ?.toMutableStateList()
        ?: mutableStateListOf()
    }

    val showConfirmDownloadDialog by remember {
      libraryViewSettings.observeShowConfirmDownload()
    }.collectAsState()

    // Live from the user row, which the socket updates when an admin changes permissions
    val currentUser by userRepository.userFlow.collectAsState()

    // The same item can appear in a playlist more than once, but it only has one progress row
    val progressKeys = playlistItems.map { it.progressKey }.distinct()
    val unfinishedKeys = progressKeys.filter { progressStates[it]?.isFinished != true }
    val finishedKeys = progressKeys.filter { progressStates[it]?.isFinished == true }
    var isUpdatingProgress by remember { mutableStateOf(false) }

    return PlaylistDetailUiState(
      name = playlistName,
      description = playlistDescription,
      currentSession = session,
      showConfirmDownloadDialog = showConfirmDownloadDialog,
      canDownload = currentUser.canDownload,
      playlistState = playlistContentState,
      playlistContentState = playlistItemsState,
      playlistItems = playlistItems,
      offlineStates = offlineStates,
      progressStates = progressStates,
      unfinishedCount = unfinishedKeys.size,
      finishedCount = finishedKeys.size,
      isUpdatingProgress = isUpdatingProgress,
      reorderSink = { fromKey, toKey ->
        val fromIndex = playlistItems.indexOfFirst { it.key == fromKey }
        val toIndex = playlistItems.indexOfFirst { it.key == toKey }
        if (fromIndex >= 0 && toIndex >= 0) {
          playlistItems.add(toIndex, playlistItems.removeAt(fromIndex))
        }
      },
    ) { event ->
      when (event) {
        PlaylistDetailUiEvent.Back -> navigator.pop()

        PlaylistDetailUiEvent.Delete -> {
          scope.launch {
            playlistsRepository.deletePlaylist(screen.playlistId)
            navigator.pop()
          }
        }

        is PlaylistDetailUiEvent.ItemClick -> {
          analytics.send(ContentSelected(ContentType.LibraryItem))
          navigator.goTo(
            LibraryItemScreen(
              libraryItemId = event.item.libraryItem.id,
              episodeId = event.item.episodeId,
              sharedTransitionKey = event.item.key + screen.playlistName,
            ),
          )
        }

        is PlaylistDetailUiEvent.PlayClick -> {
          analytics.send(ActionEvent("playlist_item", "play"))
          playbackController.startSession(
            itemId = event.item.libraryItem.id,
            episodeId = event.item.episodeId,
          )
        }

        is PlaylistDetailUiEvent.RemoveItem -> {
          analytics.send(ActionEvent("playlist_item", "deleted"))
          scope.launch {
            playlistsRepository.removeFromPlaylist(
              playlistId = screen.playlistId,
              item = event.item.asMinified(),
            )
          }
        }

        PlaylistDetailUiEvent.ReorderStopped -> {
          scope.launch {
            playlistsRepository.updatePlaylist(
              playlistId = screen.playlistId,
              name = playlistName,
              description = playlistDescription,
              items = playlistItems.map { it.asMinified() },
            )
          }
        }

        PlaylistDetailUiEvent.PlayAll -> {
          analytics.send(ActionEvent("playlist", "play"))
          scope.launch {
            val firstItem = playlistItems.firstOrNull() ?: return@launch
            val queueItems = playlistItems.drop(1)

            playbackController.startSession(
              itemId = firstItem.libraryItem.id,
              episodeId = firstItem.episodeId,
            )
            if (queueItems.isNotEmpty()) {
              sessionQueue.clear()
              // Per-item add — addAll() is book-only and would drop episodeIds.
              queueItems.forEach { item ->
                sessionQueue.add(item.libraryItem, item.episode)
              }
            }
          }
        }

        is PlaylistDetailUiEvent.DownloadAll -> {
          if (!currentUser.canDownload) return@PlaylistDetailUiState
          analytics.send(ActionEvent("playlist", "download"))
          libraryViewSettings.showConfirmDownload = !event.doNotShowAgain
          // Offline downloads are item-scoped, not episode-scoped; de-dupe podcast
          // entries that share a library item.
          val uniqueLibraryItems = playlistItems
            .map { it.libraryItem }
            .distinctBy { it.id }
          downloadManager.downloadAll(uniqueLibraryItems)
        }

        PlaylistDetailUiEvent.MarkAllFinished -> {
          if (isUpdatingProgress || unfinishedKeys.isEmpty()) return@PlaylistDetailUiState
          analytics.send(ActionEvent("playlist", "mark_finished"))
          isUpdatingProgress = true

          // Stop playback when the current item is one being finished, as marking a single
          // item finished does, so the session can't write its progress back over it
          val currentSession = session
          if (
            currentSession != null &&
            MediaProgressKey(currentSession.libraryItem.id, currentSession.episodeId) in unfinishedKeys
          ) {
            playbackController.stopSession(
              itemId = currentSession.libraryItem.id,
              episodeId = currentSession.episodeId,
            )
          }

          launchProgressUpdate(onComplete = { isUpdatingProgress = false }) {
            unfinishedKeys.forEach { sessionsRepository.markDeleted(it.libraryItemId, it.episodeId) }
            mediaProgressRepository.markAllFinished(unfinishedKeys)
            unfinishedKeys.forEach { playbackHistoryRepository.clear(it.libraryItemId, it.episodeId) }
          }
        }

        PlaylistDetailUiEvent.MarkAllNotFinished -> {
          if (isUpdatingProgress || finishedKeys.isEmpty()) return@PlaylistDetailUiState
          analytics.send(ActionEvent("playlist", "mark_not_finished"))
          isUpdatingProgress = true
          launchProgressUpdate(onComplete = { isUpdatingProgress = false }) {
            mediaProgressRepository.markAllNotFinished(finishedKeys)
          }
        }
      }
    }
  }

  /**
   * Runs in the user scope rather than the screen's, so leaving the screen part way through
   * doesn't leave the playlist half updated.
   */
  private fun launchProgressUpdate(
    onComplete: () -> Unit,
    update: suspend () -> Unit,
  ) {
    userScopeHolder.get().launch {
      try {
        update()
      } finally {
        onComplete()
      }
    }
  }
}

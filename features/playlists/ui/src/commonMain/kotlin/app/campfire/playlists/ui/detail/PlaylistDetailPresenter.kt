package app.campfire.playlists.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.toMutableStateList
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.ContentSelected
import app.campfire.analytics.events.ContentType
import app.campfire.audioplayer.PlaybackController
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.Playlist
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.playlists.api.PlaylistsRepository
import app.campfire.playlists.api.screen.PlaylistDetailScreen
import app.campfire.sessions.api.SessionQueue
import app.campfire.sessions.api.SessionsRepository
import app.campfire.settings.api.CampfireSettings
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(PlaylistDetailScreen::class, UserScope::class)
@Inject
class PlaylistDetailPresenter(
  @Assisted private val screen: PlaylistDetailScreen,
  @Assisted private val navigator: Navigator,
  private val analytics: Analytics,
  private val playlistsRepository: PlaylistsRepository,
  private val playbackController: PlaybackController,
  private val sessionsRepository: SessionsRepository,
  private val sessionQueue: SessionQueue,
  private val downloadManager: OfflineDownloadManager,
  private val settings: CampfireSettings,
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
      settings.observeShowConfirmDownload()
    }.collectAsState()

    return PlaylistDetailUiState(
      name = playlistName,
      description = playlistDescription,
      currentSession = session,
      showConfirmDownloadDialog = showConfirmDownloadDialog,
      playlistState = playlistContentState,
      playlistContentState = playlistItemsState,
      playlistItems = playlistItems,
      offlineStates = offlineStates,
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
          analytics.send(ActionEvent("playlist", "download"))
          settings.showConfirmDownload = !event.doNotShowAgain
          // Offline downloads are item-scoped, not episode-scoped; de-dupe podcast
          // entries that share a library item.
          val uniqueLibraryItems = playlistItems
            .map { it.libraryItem }
            .distinctBy { it.id }
          downloadManager.downloadAll(uniqueLibraryItems)
        }
      }
    }
  }
}

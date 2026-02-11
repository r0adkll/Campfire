package app.campfire.playlists.ui.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Playlist
import app.campfire.playlists.api.PlaylistsRepository
import app.campfire.playlists.api.dialog.PlaylistDialogResult
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias OnDismissListener = (PlaylistDialogResult) -> Unit

@Inject
class AddToPlaylistDialogPresenter(
  @Assisted private val libraryItem: LibraryItem,
  @Assisted private val onDismiss: OnDismissListener,
  private val playlistsRepository: PlaylistsRepository,
) : Presenter<AddToPlaylistViewState> {

  @Composable
  override fun present(): AddToPlaylistViewState {
    val scope = rememberCoroutineScope()

    val playlists by remember {
      playlistsRepository.observeAllPlaylists()
        .map { LoadState.Loaded(it) }
        .catch<LoadState<out List<Playlist>>> { emit(LoadState.Error) }
    }.collectAsState(LoadState.Loading)

    var loadingState by remember {
      mutableStateOf<AddLoadingState>(AddLoadingState.None)
    }

    return AddToPlaylistViewState(
      playlists = playlists,
      addLoadingState = loadingState,
      eventSink = { event ->
        when (event) {
          is AddToPlaylistViewEvent.PlaylistClicked -> {
            loadingState = AddLoadingState.Playlist(event.playlist.id)
            scope.launch {
              playlistsRepository.addToPlaylist(
                playlistId = event.playlist.id,
                item = Playlist.Item.Minified(libraryItem),
              ).onSuccess {
                onDismiss(
                  PlaylistDialogResult.Existing(
                    playlistId = event.playlist.id,
                  ),
                )
              }.onFailure {
                loadingState = AddLoadingState.Error
              }
            }
          }

          is AddToPlaylistViewEvent.CreatePlaylist -> {
            loadingState = AddLoadingState.New
            scope.launch {
              val newPlaylistId = playlistsRepository.createPlaylist(
                name = event.playlistName,
                items = listOf(Playlist.Item.Minified(libraryItem)),
              ).onSuccess { newPlaylistId ->
                onDismiss(PlaylistDialogResult.New(newPlaylistId))
              }.onFailure {
                loadingState = AddLoadingState.Error
              }
            }
          }
        }
      },
    )
  }
}

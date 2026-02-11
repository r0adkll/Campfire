package app.campfire.playlists.ui.dialog

import androidx.compose.runtime.Stable
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.Playlist
import app.campfire.core.model.PlaylistId
import com.slack.circuit.runtime.CircuitUiState

@Stable
data class AddToPlaylistViewState(
  val playlists: LoadState<out List<Playlist>>,
  val addLoadingState: AddLoadingState,
  val eventSink: (AddToPlaylistViewEvent) -> Unit,
) : CircuitUiState

@Stable
sealed interface AddLoadingState {
  data object None : AddLoadingState
  data object New : AddLoadingState
  data class Playlist(val playlistId: PlaylistId) : AddLoadingState
  data object Error : AddLoadingState

  val isLoading: Boolean
    get() = this != None && this != Error
}

sealed interface AddToPlaylistViewEvent {
  data class PlaylistClicked(val playlist: Playlist) : AddToPlaylistViewEvent
  data class CreatePlaylist(val playlistName: String) : AddToPlaylistViewEvent
}

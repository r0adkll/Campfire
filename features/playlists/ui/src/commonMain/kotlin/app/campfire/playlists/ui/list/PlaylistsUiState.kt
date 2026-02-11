package app.campfire.playlists.ui.list

import androidx.compose.runtime.Stable
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.Playlist
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Stable
data class PlaylistsUiState(
  val playlistContentState: LoadState<out List<Playlist>>,
  val eventSink: (PlaylistsUiEvent) -> Unit,
) : CircuitUiState

sealed interface PlaylistsUiEvent : CircuitUiEvent {
  data object Back : PlaylistsUiEvent
  data class PlaylistClick(val playlist: Playlist) : PlaylistsUiEvent
}

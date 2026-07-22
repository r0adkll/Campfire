package app.campfire.playlists.ui.list

import androidx.compose.runtime.Stable
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.Playlist
import app.campfire.core.settings.GroupDisplayState
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Stable
data class PlaylistsUiState(
  val playlistContentState: LoadState<out List<Playlist>>,
  val displayState: GroupDisplayState,
  val eventSink: (PlaylistsUiEvent) -> Unit,
) : CircuitUiState

sealed interface PlaylistsUiEvent : CircuitUiEvent {
  data object Back : PlaylistsUiEvent
  data object ToggleDisplayState : PlaylistsUiEvent
  data class PlaylistClick(val playlist: Playlist) : PlaylistsUiEvent
}

package app.campfire.playlists.ui.detail

import androidx.compose.runtime.Stable
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.Playlist
import app.campfire.core.model.Session
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Stable
data class PlaylistDetailUiState(
  val name: String,
  val description: String?,
  val currentSession: Session?,
  val showConfirmDownloadDialog: Boolean,
  val playlistState: LoadState<out Playlist>,
  val playlistContentState: LoadState<out List<LibraryItem>>,
  val playlistItems: List<LibraryItem>,
  val offlineStates: Map<LibraryItemId, OfflineDownload>,
  val reorderSink: suspend (from: LibraryItemId, to: LibraryItemId) -> Unit,
  val eventSink: (PlaylistDetailUiEvent) -> Unit,
) : CircuitUiState

sealed interface PlaylistDetailUiEvent : CircuitUiEvent {
  data object Back : PlaylistDetailUiEvent
  data object Delete : PlaylistDetailUiEvent
  data class ItemClick(val libraryItem: LibraryItem) : PlaylistDetailUiEvent
  data class PlayClick(val libraryItem: LibraryItem) : PlaylistDetailUiEvent
  data class RemoveItem(val libraryItem: LibraryItem) : PlaylistDetailUiEvent
  data class DownloadAll(val doNotShowAgain: Boolean = true) : PlaylistDetailUiEvent

  data object PlayAll : PlaylistDetailUiEvent
  data object ReorderStopped : PlaylistDetailUiEvent
}

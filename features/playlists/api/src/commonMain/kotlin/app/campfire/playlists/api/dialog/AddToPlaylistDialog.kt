package app.campfire.playlists.api.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.PlaylistId

sealed interface PlaylistDialogResult {
  data object None : PlaylistDialogResult
  data class Existing(val playlistId: PlaylistId) : PlaylistDialogResult
  data class New(val playlistId: PlaylistId) : PlaylistDialogResult
}

interface AddToPlaylistDialog {

  @Composable
  fun Content(
    libraryItem: LibraryItem,
    onDismiss: (PlaylistDialogResult) -> Unit,
    modifier: Modifier,
  )

  companion object NoOp : AddToPlaylistDialog {
    @Composable
    override fun Content(
      libraryItem: LibraryItem,
      onDismiss: (PlaylistDialogResult) -> Unit,
      modifier: Modifier,
    ) {
    }
  }
}

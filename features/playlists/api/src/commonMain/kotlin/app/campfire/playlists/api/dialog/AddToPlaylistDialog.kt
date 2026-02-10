package app.campfire.playlists.api.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.core.model.LibraryItem

interface AddToPlaylistDialog {

  @Composable
  fun Content(
    libraryItem: LibraryItem,
    onDismiss: () -> Unit,
    modifier: Modifier,
  )
}

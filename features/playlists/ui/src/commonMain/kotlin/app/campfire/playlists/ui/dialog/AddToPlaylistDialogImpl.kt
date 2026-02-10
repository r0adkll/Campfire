package app.campfire.playlists.ui.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItem
import app.campfire.playlists.api.PlaylistsRepository
import app.campfire.playlists.api.dialog.AddToPlaylistDialog
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

@ContributesBinding(UserScope::class)
@Inject
class AddToPlaylistDialogImpl(
  private val playlistsRepository: PlaylistsRepository,
) : AddToPlaylistDialog {

  @Composable
  override fun Content(
    libraryItem: LibraryItem,
    onDismiss: () -> Unit,
    modifier: Modifier,
  ) {

  }
}

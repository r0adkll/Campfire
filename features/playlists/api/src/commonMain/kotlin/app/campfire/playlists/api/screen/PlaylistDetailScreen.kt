package app.campfire.playlists.api.screen

import app.campfire.common.screens.BaseScreen
import app.campfire.common.screens.Presentation
import app.campfire.core.model.Playlist
import app.campfire.core.model.PlaylistId
import app.campfire.core.parcelize.Parcelize

@Parcelize
class PlaylistDetailScreen(
  val playlistId: PlaylistId,
  val playlistName: String?,
  val playlistDescription: String?,
  val isCreatedId: Boolean = false,
) : BaseScreen(name = "PlaylistDetail") {
  constructor(playlist: Playlist) : this(
    playlistId = playlist.id,
    playlistName = playlist.name,
    playlistDescription = playlist.description,
  )

  override val presentation: Presentation
    get() = Presentation.Fullscreen
}

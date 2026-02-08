package app.campfire.playlists.api.screen

import app.campfire.common.screens.BaseScreen
import app.campfire.core.model.Playlist
import app.campfire.core.parcelize.Parcelize

@Parcelize
class PlaylistDetailScreen(
  val playlistId: String,
  val playlistName: String,
  val playlistDescription: String?,
) : BaseScreen(name = "PlaylistDetail") {
  constructor(playlist: Playlist) : this(
    playlistId = playlist.id,
    playlistName = playlist.name,
    playlistDescription = playlist.description,
  )
}

package app.campfire.playlists.api.screen

import app.campfire.common.screens.DetailScreen
import app.campfire.core.model.Playlist
import app.campfire.core.model.PlaylistId
import app.campfire.core.parcelize.Parcelize

@Parcelize
class PlaylistDetailScreen(
  val playlistId: PlaylistId,
  val playlistName: String?,
  val playlistDescription: String?,
  val isCreatedId: Boolean = false,
) : DetailScreen(name = "PlaylistDetail") {
  constructor(playlist: Playlist) : this(
    playlistId = playlist.id,
    playlistName = playlist.name,
    playlistDescription = playlist.description,
  )
}

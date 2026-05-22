package app.campfire.socket.events

import app.campfire.network.RequestOrigin
import app.campfire.network.models.Playlist
import app.campfire.network.models.PlaylistExpanded
import app.campfire.network.models.PlaylistItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class PlaylistAdded(
  val playlist: PlaylistExpanded,
) : SocketEvent {
  override fun toString(): String =
    "PlaylistAdded(id=${playlist.id}, name=${playlist.name}, items=${playlist.items.size})"

  override fun applyOrigin(origin: RequestOrigin) {
    playlist.applyOrigin(origin)
  }

  companion object : SocketEventConfig<PlaylistAdded> {
    override val name: String = "playlist_added"
    override fun Json.decode(element: JsonElement): PlaylistAdded {
      return PlaylistAdded(decodeFromJsonElement(Playlist.serializer(PlaylistItem.Expanded.serializer()), element))
    }
  }
}

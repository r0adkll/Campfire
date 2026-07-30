// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.socket.events

import app.campfire.network.RequestOrigin
import app.campfire.network.models.Playlist
import app.campfire.network.models.PlaylistExpanded
import app.campfire.network.models.PlaylistItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class PlaylistUpdated(
  val playlist: PlaylistExpanded,
) : SocketEvent {
  override fun toString(): String =
    "PlaylistUpdated(id=${playlist.id}, name=${playlist.name}, items=${playlist.items.size})"

  override fun applyOrigin(origin: RequestOrigin) {
    playlist.applyOrigin(origin)
  }

  companion object : SocketEventConfig<PlaylistUpdated> {
    override val name: String = "playlist_updated"
    override fun Json.decode(element: JsonElement): PlaylistUpdated {
      return PlaylistUpdated(decodeFromJsonElement(Playlist.serializer(PlaylistItem.Expanded.serializer()), element))
    }
  }
}

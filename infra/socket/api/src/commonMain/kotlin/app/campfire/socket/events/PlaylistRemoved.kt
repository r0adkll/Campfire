// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.socket.events

import app.campfire.network.RequestOrigin
import app.campfire.network.models.Playlist
import app.campfire.network.models.PlaylistExpanded
import app.campfire.network.models.PlaylistItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class PlaylistRemoved(
  val playlist: PlaylistExpanded,
) : SocketEvent {
  override fun toString(): String = "PlaylistRemoved(id=${playlist.id}, name=${playlist.name})"

  override fun applyOrigin(origin: RequestOrigin) {
    playlist.applyOrigin(origin)
  }

  companion object : SocketEventConfig<PlaylistRemoved> {
    override val name: String = "playlist_removed"
    override fun Json.decode(element: JsonElement): PlaylistRemoved {
      return PlaylistRemoved(decodeFromJsonElement(Playlist.serializer(PlaylistItem.Expanded.serializer()), element))
    }
  }
}

// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.playlists.socket

import app.campfire.core.di.UserScope
import app.campfire.socket.events.PlaylistAdded
import app.campfire.socket.events.PlaylistRemoved
import app.campfire.socket.events.PlaylistUpdated
import app.campfire.socket.events.SocketEvent
import app.campfire.socket.events.SocketEventListener
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import me.tatarka.inject.annotations.Inject

@ContributesMultibinding(UserScope::class, boundType = SocketEventListener::class)
@Inject
class PlaylistCacheInvalidationListener(
  private val handler: PlaylistEventHandler,
) : SocketEventListener {
  override suspend fun handle(event: SocketEvent) {
    when (event) {
      is PlaylistAdded -> handler.onPlaylistAdded(event.playlist)
      is PlaylistUpdated -> handler.onPlaylistUpdated(event.playlist)
      is PlaylistRemoved -> handler.onPlaylistRemoved(event.playlist.id)
      else -> Unit
    }
  }
}

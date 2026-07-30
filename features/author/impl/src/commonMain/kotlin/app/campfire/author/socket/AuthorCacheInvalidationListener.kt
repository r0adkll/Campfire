// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.author.socket

import app.campfire.core.di.UserScope
import app.campfire.socket.events.AuthorAdded
import app.campfire.socket.events.AuthorRemoved
import app.campfire.socket.events.AuthorUpdated
import app.campfire.socket.events.SocketEvent
import app.campfire.socket.events.SocketEventListener
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import me.tatarka.inject.annotations.Inject

@ContributesMultibinding(UserScope::class, boundType = SocketEventListener::class)
@Inject
class AuthorCacheInvalidationListener(
  private val handler: AuthorEventHandler,
) : SocketEventListener {
  override suspend fun handle(event: SocketEvent) {
    when (event) {
      is AuthorAdded -> handler.onAuthorAdded(event.author)
      is AuthorUpdated -> handler.onAuthorUpdated(event.author)
      is AuthorRemoved -> handler.onAuthorRemoved(event.payload.id)
      else -> Unit
    }
  }
}

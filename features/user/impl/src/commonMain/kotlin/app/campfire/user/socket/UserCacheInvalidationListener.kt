// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.user.socket

import app.campfire.core.di.UserScope
import app.campfire.socket.events.SocketEvent
import app.campfire.socket.events.SocketEventListener
import app.campfire.socket.events.UserUpdated
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@ContributesIntoSet(UserScope::class, binding = binding<SocketEventListener>())
@Inject
class UserCacheInvalidationListener(
  private val handler: UserEventHandler,
) : SocketEventListener {
  override suspend fun handle(event: SocketEvent) {
    if (event !is UserUpdated) return
    handler.onUserUpdated(event.user)
  }
}

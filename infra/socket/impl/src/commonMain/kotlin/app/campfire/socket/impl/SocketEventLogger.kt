// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.socket.impl

import app.campfire.core.di.UserScope
import app.campfire.core.logging.Corked
import app.campfire.socket.events.SocketEvent
import app.campfire.socket.events.SocketEventListener
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@ContributesIntoSet(UserScope::class, binding = binding<SocketEventListener>())
@Inject
class SocketEventLogger : SocketEventListener {

  companion object : Corked("SocketEventLogger")

  override suspend fun handle(event: SocketEvent) {
    ibark { "Socket event: $event" }
  }
}

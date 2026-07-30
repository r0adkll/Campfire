// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.socket.impl

import app.campfire.core.di.UserScope
import app.campfire.core.logging.Corked
import app.campfire.socket.events.SocketEvent
import app.campfire.socket.events.SocketEventListener
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import me.tatarka.inject.annotations.Inject

@ContributesMultibinding(UserScope::class, boundType = SocketEventListener::class)
@Inject
class SocketEventLogger : SocketEventListener {

  companion object : Corked("SocketEventLogger")

  override suspend fun handle(event: SocketEvent) {
    ibark { "Socket event: $event" }
  }
}

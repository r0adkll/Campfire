// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.socket.impl

import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.Scoped
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.Corked
import app.campfire.socket.SocketManager
import app.campfire.socket.events.SocketEventListener
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.launch

@ContributesIntoSet(UserScope::class, binding = binding<Scoped>())
@Inject
class SocketEventDispatcher(
  private val socketManager: SocketManager,
  private val listeners: Set<SocketEventListener>,
  @ForScope(UserScope::class) private val coroutineScopeHolder: CoroutineScopeHolder,
) : Scoped {

  companion object : Corked("SocketEventDispatcher")

  override suspend fun onCreate() {
    val scope = coroutineScopeHolder.get()
    listeners.forEach { listener ->
      scope.launch {
        socketManager.events.collect { event ->
          runCatching { listener.handle(event) }
            .onFailure { ebark(it) { "${listener::class.simpleName} threw on $event" } }
        }
      }
    }
  }
}

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
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@ContributesMultibinding(UserScope::class, boundType = Scoped::class)
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

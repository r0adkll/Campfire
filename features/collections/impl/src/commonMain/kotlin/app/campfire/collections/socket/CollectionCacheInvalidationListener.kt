// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.collections.socket

import app.campfire.core.di.UserScope
import app.campfire.socket.events.CollectionAdded
import app.campfire.socket.events.CollectionRemoved
import app.campfire.socket.events.CollectionUpdated
import app.campfire.socket.events.SocketEvent
import app.campfire.socket.events.SocketEventListener
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@ContributesIntoSet(UserScope::class, binding = binding<SocketEventListener>())
@Inject
class CollectionCacheInvalidationListener(
  private val handler: CollectionEventHandler,
) : SocketEventListener {
  override suspend fun handle(event: SocketEvent) {
    when (event) {
      is CollectionAdded -> handler.onCollectionAdded(event.collection)
      is CollectionUpdated -> handler.onCollectionUpdated(event.collection)
      is CollectionRemoved -> handler.onCollectionRemoved(event.collection.id)
      else -> Unit
    }
  }
}

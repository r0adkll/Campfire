// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.socket.events

import app.campfire.network.RequestOrigin
import app.campfire.network.models.Collection
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class CollectionAdded(
  val collection: Collection,
) : SocketEvent {
  override fun toString(): String =
    "CollectionAdded(id=${collection.id}, name=${collection.name}, books=${collection.books.size})"

  override fun applyOrigin(origin: RequestOrigin) {
    collection.applyOrigin(origin)
  }

  companion object : SocketEventConfig<CollectionAdded> {
    override val name: String = "collection_added"
    override fun Json.decode(element: JsonElement): CollectionAdded {
      return CollectionAdded(decodeFromJsonElement(Collection.serializer(), element))
    }
  }
}

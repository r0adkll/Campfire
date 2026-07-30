// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.socket.events

import app.campfire.network.RequestOrigin
import app.campfire.network.models.LibraryItemExpanded
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class ItemAdded(
  val item: LibraryItemExpanded,
) : SocketEvent {
  override fun toString(): String = "ItemAdded(id=${item.id}, libraryId=${item.libraryId})"

  override fun applyOrigin(origin: RequestOrigin) {
    item.applyOrigin(origin)
  }

  companion object : SocketEventConfig<ItemAdded> {
    override val name: String = "item_added"
    override fun Json.decode(element: JsonElement): ItemAdded {
      return ItemAdded(decodeFromJsonElement(LibraryItemExpanded.serializer(), element))
    }
  }
}

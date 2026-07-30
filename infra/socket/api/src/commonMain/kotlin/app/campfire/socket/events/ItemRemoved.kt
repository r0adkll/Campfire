// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.socket.events

import app.campfire.socket.payloads.ItemRemovedPayload
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class ItemRemoved(
  val payload: ItemRemovedPayload,
) : SocketEvent {
  override fun toString(): String = "ItemRemoved(id=${payload.id}, libraryId=${payload.libraryId})"

  companion object : SocketEventConfig<ItemRemoved> {
    override val name: String = "item_removed"
    override fun Json.decode(element: JsonElement): ItemRemoved {
      return ItemRemoved(decodeFromJsonElement(ItemRemovedPayload.serializer(), element))
    }
  }
}

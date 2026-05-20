package app.campfire.socket.events

import app.campfire.network.models.LibraryItemExpanded
import app.campfire.socket.handlers.SocketEventHandler
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class ItemAdded(
  val item: LibraryItemExpanded,
) : SocketEvent {
  override fun toString(): String = "ItemAdded(id=${item.id}, libraryId=${item.libraryId})"

  companion object : SocketEventHandler<ItemAdded> {
    override val name: String = "item_added"
    override fun Json.decode(element: JsonElement): ItemAdded {
      return ItemAdded(decodeFromJsonElement(LibraryItemExpanded.serializer(), element))
    }
  }
}

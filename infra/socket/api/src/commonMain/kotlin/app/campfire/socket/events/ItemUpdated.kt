package app.campfire.socket.events

import app.campfire.network.models.LibraryItemExpanded
import app.campfire.socket.handlers.SocketEventHandler
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class ItemUpdated(
  val item: LibraryItemExpanded,
) : SocketEvent {
  override fun toString(): String = "ItemUpdated(id=${item.id}, libraryId=${item.libraryId})"

  companion object : SocketEventHandler<ItemUpdated> {
    override val name: String = "item_updated"
    override fun Json.decode(element: JsonElement): ItemUpdated {
      return ItemUpdated(decodeFromJsonElement(LibraryItemExpanded.serializer(), element))
    }
  }
}

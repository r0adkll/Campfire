package app.campfire.socket.events

import app.campfire.network.models.LibraryItemExpanded
import app.campfire.socket.handlers.SocketEventHandler
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class ItemsUpdated(
  val items: List<LibraryItemExpanded>,
) : SocketEvent {
  override fun toString(): String = "ItemsUpdated(count=${items.size})"

  companion object : SocketEventHandler<ItemsUpdated> {
    override val name: String = "items_updated"
    override fun Json.decode(element: JsonElement): ItemsUpdated {
      return ItemsUpdated(decodeFromJsonElement(ListSerializer(LibraryItemExpanded.serializer()), element))
    }
  }
}

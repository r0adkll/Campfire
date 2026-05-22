package app.campfire.socket.events

import app.campfire.network.RequestOrigin
import app.campfire.network.models.LibraryItemExpanded
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class ItemsUpdated(
  val items: List<LibraryItemExpanded>,
) : SocketEvent {
  override fun toString(): String = "ItemsUpdated(count=${items.size})"

  override fun applyOrigin(origin: RequestOrigin) {
    items.forEach { it.applyOrigin(origin) }
  }

  companion object : SocketEventConfig<ItemsUpdated> {
    override val name: String = "items_updated"
    override fun Json.decode(element: JsonElement): ItemsUpdated {
      return ItemsUpdated(decodeFromJsonElement(ListSerializer(LibraryItemExpanded.serializer()), element))
    }
  }
}

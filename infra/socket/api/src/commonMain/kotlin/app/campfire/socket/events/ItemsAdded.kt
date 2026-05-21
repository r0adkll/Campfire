package app.campfire.socket.events

import app.campfire.network.models.LibraryItemExpanded
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class ItemsAdded(
  val items: List<LibraryItemExpanded>,
) : SocketEvent {
  override fun toString(): String = "ItemsAdded(count=${items.size})"

  companion object : SocketEventConfig<ItemsAdded> {
    override val name: String = "items_added"
    override fun Json.decode(element: JsonElement): ItemsAdded {
      return ItemsAdded(decodeFromJsonElement(ListSerializer(LibraryItemExpanded.serializer()), element))
    }
  }
}

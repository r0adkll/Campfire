package app.campfire.socket.events

import app.campfire.network.RequestOrigin
import app.campfire.network.models.Collection
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class CollectionUpdated(
  val collection: Collection,
) : SocketEvent {
  override fun toString(): String =
    "CollectionUpdated(id=${collection.id}, name=${collection.name}, books=${collection.books.size})"

  override fun applyOrigin(origin: RequestOrigin) {
    collection.applyOrigin(origin)
  }

  companion object : SocketEventConfig<CollectionUpdated> {
    override val name: String = "collection_updated"
    override fun Json.decode(element: JsonElement): CollectionUpdated {
      return CollectionUpdated(decodeFromJsonElement(Collection.serializer(), element))
    }
  }
}

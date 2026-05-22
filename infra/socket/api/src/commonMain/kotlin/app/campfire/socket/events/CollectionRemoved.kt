package app.campfire.socket.events

import app.campfire.network.RequestOrigin
import app.campfire.network.models.Collection
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class CollectionRemoved(
  val collection: Collection,
) : SocketEvent {
  override fun toString(): String = "CollectionRemoved(id=${collection.id}, name=${collection.name})"

  override fun applyOrigin(origin: RequestOrigin) {
    collection.applyOrigin(origin)
  }

  companion object : SocketEventConfig<CollectionRemoved> {
    override val name: String = "collection_removed"
    override fun Json.decode(element: JsonElement): CollectionRemoved {
      return CollectionRemoved(decodeFromJsonElement(Collection.serializer(), element))
    }
  }
}

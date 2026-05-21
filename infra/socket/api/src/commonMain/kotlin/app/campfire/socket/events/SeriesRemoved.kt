package app.campfire.socket.events

import app.campfire.socket.payloads.SeriesRemovedPayload
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class SeriesRemoved(
  val payload: SeriesRemovedPayload,
) : SocketEvent {
  override fun toString(): String = "SeriesRemoved(id=${payload.id}, libraryId=${payload.libraryId})"

  companion object : SocketEventConfig<SeriesRemoved> {
    override val name: String = "series_removed"
    override fun Json.decode(element: JsonElement): SeriesRemoved {
      return SeriesRemoved(decodeFromJsonElement(SeriesRemovedPayload.serializer(), element))
    }
  }
}

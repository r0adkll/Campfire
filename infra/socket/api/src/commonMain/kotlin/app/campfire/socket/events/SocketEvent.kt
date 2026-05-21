package app.campfire.socket.events

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

sealed interface SocketEvent

interface SocketEventConfig<EventT : SocketEvent> {
  val name: String

  fun Json.decode(element: JsonElement): EventT
}

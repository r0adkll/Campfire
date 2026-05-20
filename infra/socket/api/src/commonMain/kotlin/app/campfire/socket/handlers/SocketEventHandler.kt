package app.campfire.socket.handlers

import app.campfire.socket.events.SocketEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

interface SocketEventHandler<EventT : SocketEvent> {
  val name: String

  fun Json.decode(element: JsonElement): EventT
}

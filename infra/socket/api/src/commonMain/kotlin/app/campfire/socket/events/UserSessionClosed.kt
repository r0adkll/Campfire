package app.campfire.socket.events

import app.campfire.socket.handlers.SocketEventHandler
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

data class UserSessionClosed(
  val sessionId: String,
) : SocketEvent {
  override fun toString(): String = "UserSessionClosed(sessionId=$sessionId)"

  companion object : SocketEventHandler<UserSessionClosed> {
    override val name: String = "user_session_closed"
    override fun Json.decode(element: JsonElement): UserSessionClosed {
      val sessionId = (element as JsonPrimitive).content
      return UserSessionClosed(sessionId)
    }
  }
}

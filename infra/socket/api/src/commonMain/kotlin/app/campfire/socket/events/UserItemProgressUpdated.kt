package app.campfire.socket.events

import app.campfire.socket.handlers.SocketEventHandler
import app.campfire.socket.payloads.UserItemProgressUpdatedPayload
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class UserItemProgressUpdated(
  val payload: UserItemProgressUpdatedPayload,
) : SocketEvent {
  override fun toString(): String =
    "UserItemProgressUpdated(id=${payload.id}, progress=${payload.data.progress})"

  companion object : SocketEventHandler<UserItemProgressUpdated> {
    override val name: String = "user_item_progress_updated"
    override fun Json.decode(element: JsonElement): UserItemProgressUpdated {
      return UserItemProgressUpdated(decodeFromJsonElement(UserItemProgressUpdatedPayload.serializer(), element))
    }
  }
}

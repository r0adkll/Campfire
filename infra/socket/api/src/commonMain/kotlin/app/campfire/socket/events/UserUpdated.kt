package app.campfire.socket.events

import app.campfire.network.models.User
import app.campfire.socket.handlers.SocketEventHandler
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class UserUpdated(
  val user: User,
) : SocketEvent {
  override fun toString(): String = "UserUpdated(id=${user.id}, username=${user.username})"

  companion object : SocketEventHandler<UserUpdated> {
    override val name: String = "user_updated"
    override fun Json.decode(element: JsonElement): UserUpdated {
      return UserUpdated(decodeFromJsonElement(User.serializer(), element))
    }
  }
}

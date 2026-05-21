package app.campfire.socket.events

import app.campfire.socket.payloads.AuthorRemovedPayload
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class AuthorRemoved(
  val payload: AuthorRemovedPayload,
) : SocketEvent {
  override fun toString(): String = "AuthorRemoved(id=${payload.id}, libraryId=${payload.libraryId})"

  companion object : SocketEventConfig<AuthorRemoved> {
    override val name: String = "author_removed"
    override fun Json.decode(element: JsonElement): AuthorRemoved {
      return AuthorRemoved(decodeFromJsonElement(AuthorRemovedPayload.serializer(), element))
    }
  }
}

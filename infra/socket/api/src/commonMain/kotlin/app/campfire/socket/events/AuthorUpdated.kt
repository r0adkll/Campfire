package app.campfire.socket.events

import app.campfire.network.models.Author
import app.campfire.socket.handlers.SocketEventHandler
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class AuthorUpdated(
  val author: Author,
) : SocketEvent {
  override fun toString(): String = "AuthorUpdated(id=${author.id}, name=${author.name})"

  companion object : SocketEventHandler<AuthorUpdated> {
    override val name: String = "author_updated"
    override fun Json.decode(element: JsonElement): AuthorUpdated {
      return AuthorUpdated(decodeFromJsonElement(Author.serializer(), element))
    }
  }
}

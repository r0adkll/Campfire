package app.campfire.socket.events

import app.campfire.network.models.Author
import app.campfire.socket.handlers.SocketEventHandler
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class AuthorAdded(
  val author: Author,
) : SocketEvent {
  override fun toString(): String = "AuthorAdded(id=${author.id}, name=${author.name})"

  companion object : SocketEventHandler<AuthorAdded> {
    override val name: String = "author_added"
    override fun Json.decode(element: JsonElement): AuthorAdded {
      return AuthorAdded(decodeFromJsonElement(Author.serializer(), element))
    }
  }
}

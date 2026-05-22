package app.campfire.socket.events

import app.campfire.network.RequestOrigin
import app.campfire.network.models.Author
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class AuthorAdded(
  val author: Author,
) : SocketEvent {
  override fun toString(): String = "AuthorAdded(id=${author.id}, name=${author.name})"

  override fun applyOrigin(origin: RequestOrigin) {
    author.applyOrigin(origin)
  }

  companion object : SocketEventConfig<AuthorAdded> {
    override val name: String = "author_added"
    override fun Json.decode(element: JsonElement): AuthorAdded {
      return AuthorAdded(decodeFromJsonElement(Author.serializer(), element))
    }
  }
}

package app.campfire.socket.events

import app.campfire.network.models.Library
import app.campfire.socket.handlers.SocketEventHandler
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class LibraryAdded(
  val library: Library,
) : SocketEvent {
  override fun toString(): String = "LibraryAdded(id=${library.id}, name=${library.name})"

  companion object : SocketEventHandler<LibraryAdded> {
    override val name: String = "library_added"
    override fun Json.decode(element: JsonElement): LibraryAdded {
      return LibraryAdded(decodeFromJsonElement(Library.serializer(), element))
    }
  }
}

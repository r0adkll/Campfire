package app.campfire.socket.events

import app.campfire.network.models.Library
import app.campfire.socket.handlers.SocketEventHandler
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class LibraryUpdated(
  val library: Library,
) : SocketEvent {
  override fun toString(): String = "LibraryUpdated(id=${library.id}, name=${library.name})"

  companion object : SocketEventHandler<LibraryUpdated> {
    override val name: String = "library_updated"
    override fun Json.decode(element: JsonElement): LibraryUpdated {
      return LibraryUpdated(decodeFromJsonElement(Library.serializer(), element))
    }
  }
}

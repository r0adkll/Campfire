package app.campfire.socket.events

import app.campfire.network.models.Library
import app.campfire.socket.handlers.SocketEventHandler
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class LibraryRemoved(
  val library: Library,
) : SocketEvent {
  override fun toString(): String = "LibraryRemoved(id=${library.id}, name=${library.name})"

  companion object : SocketEventHandler<LibraryRemoved> {
    override val name: String = "library_removed"
    override fun Json.decode(element: JsonElement): LibraryRemoved {
      return LibraryRemoved(decodeFromJsonElement(Library.serializer(), element))
    }
  }
}

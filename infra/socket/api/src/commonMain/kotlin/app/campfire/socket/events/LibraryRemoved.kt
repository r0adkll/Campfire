// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.socket.events

import app.campfire.network.RequestOrigin
import app.campfire.network.models.Library
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class LibraryRemoved(
  val library: Library,
) : SocketEvent {
  override fun toString(): String = "LibraryRemoved(id=${library.id}, name=${library.name})"

  override fun applyOrigin(origin: RequestOrigin) {
    library.applyOrigin(origin)
  }

  companion object : SocketEventConfig<LibraryRemoved> {
    override val name: String = "library_removed"
    override fun Json.decode(element: JsonElement): LibraryRemoved {
      return LibraryRemoved(decodeFromJsonElement(Library.serializer(), element))
    }
  }
}

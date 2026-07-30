// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.socket.events

import app.campfire.network.RequestOrigin
import app.campfire.network.models.Author
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class AuthorUpdated(
  val author: Author,
) : SocketEvent {
  override fun toString(): String = "AuthorUpdated(id=${author.id}, name=${author.name})"

  override fun applyOrigin(origin: RequestOrigin) {
    author.applyOrigin(origin)
  }

  companion object : SocketEventConfig<AuthorUpdated> {
    override val name: String = "author_updated"
    override fun Json.decode(element: JsonElement): AuthorUpdated {
      return AuthorUpdated(decodeFromJsonElement(Author.serializer(), element))
    }
  }
}

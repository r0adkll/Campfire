// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.socket.events

import app.campfire.network.models.User
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class UserUpdated(
  val user: User,
) : SocketEvent {
  override fun toString(): String = "UserUpdated(id=${user.id}, username=${user.username})"

  companion object : SocketEventConfig<UserUpdated> {
    override val name: String = "user_updated"
    override fun Json.decode(element: JsonElement): UserUpdated {
      return UserUpdated(decodeFromJsonElement(User.serializer(), element))
    }
  }
}

// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.socket.events

import app.campfire.network.RequestOrigin
import app.campfire.socket.payloads.UserItemProgressUpdatedPayload
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class UserItemProgressUpdated(
  val payload: UserItemProgressUpdatedPayload,
) : SocketEvent {
  override fun toString(): String =
    "UserItemProgressUpdated(id=${payload.id}, progress=${payload.data.progress})"

  override fun applyOrigin(origin: RequestOrigin) {
    payload.data.applyOrigin(origin)
  }

  companion object : SocketEventConfig<UserItemProgressUpdated> {
    override val name: String = "user_item_progress_updated"
    override fun Json.decode(element: JsonElement): UserItemProgressUpdated {
      return UserItemProgressUpdated(decodeFromJsonElement(UserItemProgressUpdatedPayload.serializer(), element))
    }
  }
}

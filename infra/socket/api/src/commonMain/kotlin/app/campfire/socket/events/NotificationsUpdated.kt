// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.socket.events

import app.campfire.network.models.NotificationSettings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class NotificationsUpdated(
  val settings: NotificationSettings,
) : SocketEvent {
  override fun toString(): String =
    "NotificationsUpdated(id=${settings.id}, count=${settings.notifications?.size ?: 0})"

  companion object : SocketEventConfig<NotificationsUpdated> {
    override val name: String = "notifications_updated"
    override fun Json.decode(element: JsonElement): NotificationsUpdated {
      return NotificationsUpdated(decodeFromJsonElement(NotificationSettings.serializer(), element))
    }
  }
}

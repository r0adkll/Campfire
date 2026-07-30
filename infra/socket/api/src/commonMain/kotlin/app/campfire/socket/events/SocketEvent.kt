// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.socket.events

import app.campfire.network.RequestOrigin
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

sealed interface SocketEvent {
  /**
   * Propagate the event's [RequestOrigin] into any wire-model payloads it carries (anything
   * extending `NetworkModel` or `Envelope`). Default is a no-op for events with plain payloads.
   * Called by [app.campfire.socket.impl.DefaultSocketManager] immediately after decoding so
   * downstream DB inserts (which inspect `NetworkModel.origin`) see the right server URL.
   */
  fun applyOrigin(origin: RequestOrigin) = Unit
}

interface SocketEventConfig<EventT : SocketEvent> {
  val name: String

  fun Json.decode(element: JsonElement): EventT
}

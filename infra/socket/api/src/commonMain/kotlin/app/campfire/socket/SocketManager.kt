// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.socket

import app.campfire.socket.events.SocketEvent
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface SocketManager {
  val state: StateFlow<SocketState>
  val events: SharedFlow<SocketEvent>

  fun retryConnection()
}

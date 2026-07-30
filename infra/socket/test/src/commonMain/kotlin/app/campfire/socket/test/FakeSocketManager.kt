// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.socket.test

import app.campfire.socket.SocketManager
import app.campfire.socket.SocketState
import app.campfire.socket.events.SocketEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSocketManager : SocketManager {
  override val state: MutableStateFlow<SocketState> =
    MutableStateFlow(SocketState.Disconnected)

  override val events: MutableSharedFlow<SocketEvent> =
    MutableSharedFlow(extraBufferCapacity = 64)

  override fun retryConnection() {
    // Do nothing
  }
}

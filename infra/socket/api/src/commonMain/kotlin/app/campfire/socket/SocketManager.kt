package app.campfire.socket

import app.campfire.socket.events.SocketEvent
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface SocketManager {
  val state: StateFlow<SocketState>
  val events: SharedFlow<SocketEvent>

  fun retryConnection()
}

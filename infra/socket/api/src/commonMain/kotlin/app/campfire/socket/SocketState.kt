package app.campfire.socket

sealed interface SocketState {
  data object Disconnected : SocketState
  data object Connecting : SocketState
  data object Authenticating : SocketState
  data class Authenticated(val userId: String, val username: String) : SocketState
  data class Failed(val reason: String) : SocketState
}

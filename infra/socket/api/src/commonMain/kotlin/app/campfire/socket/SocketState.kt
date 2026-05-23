package app.campfire.socket

sealed interface SocketState {
  data object Disconnected : SocketState
  data object Connecting : SocketState
  data object Authenticating : SocketState
  data class Authenticated(val userId: String, val username: String) : SocketState
  data class Failed(val reason: String) : SocketState

  /**
   * The user has turned realtime sync off via settings. The socket is intentionally idle and
   * UI indicators should hide rather than render a "disconnected" state — being disabled is
   * not a fault condition.
   */
  data object Disabled : SocketState
}

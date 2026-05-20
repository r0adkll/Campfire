package app.campfire.socket

data class RawSocketEvent(
  val name: String,
  val args: List<Any?>,
)

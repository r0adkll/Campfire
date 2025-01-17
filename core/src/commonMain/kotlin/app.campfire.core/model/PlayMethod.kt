package app.campfire.core.model

enum class PlayMethod(val serverValue: Int) {
  DirectPlay(0),
  DirectStream(1),
  Transcode(2),
  Local(3),
}

package app.campfire.audioplayer.model

sealed interface PlaybackTimer {
  data object EndOfChapter : PlaybackTimer
  data class Epoch(val epochMillis: Long) : PlaybackTimer
}

data class RunningTimer(
  val timer: PlaybackTimer,
  val startedAt: Long,
)

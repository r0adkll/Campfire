package app.campfire.audioplayer.model

sealed interface PlaybackTimer {
  val isAutoSleepTimer: Boolean

  data class EndOfChapter(
    override val isAutoSleepTimer: Boolean = false,
  ) : PlaybackTimer

  data class Epoch(
    val epochMillis: Long,
    override val isAutoSleepTimer: Boolean = false,
  ) : PlaybackTimer
}

data class RunningTimer(
  val timer: PlaybackTimer,
  val startedAt: Long,
  val isShakeToRestartEnabled: Boolean,
)

package app.campfire.audioplayer.impl.sleep

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import kotlinx.coroutines.flow.StateFlow

interface SleepTimerManager {

  val runningTimer: StateFlow<RunningTimer?>

  fun onSessionStart()
  fun setTimer(timer: PlaybackTimer)
  fun clearTimer()
  fun endOfChapter(): Boolean

  interface Factory {
    fun create(
      player: AudioPlayer,
    ): SleepTimerManager
  }
}

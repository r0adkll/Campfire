package app.campfire.audioplayer.impl.sleep

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.time.FatherTime
import app.campfire.settings.api.SleepSettings
import app.campfire.shake.ShakeDetector
import app.campfire.shake.ShakeSensitivity
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@Inject
class CoroutineSleepTimerManager(
  @Assisted private val player: AudioPlayer,
  private val sleepSettings: SleepSettings,
  private val shakeDetector: ShakeDetector,
  private val dispatcherProvider: DispatcherProvider,
  private val fatherTime: FatherTime,
  @ForScope(AppScope::class) private val applicationScope: CoroutineScope,
) : SleepTimerManager {

  @ContributesBinding(AppScope::class)
  @Inject
  class Factory(
    private val managerFactory: (AudioPlayer) -> CoroutineSleepTimerManager,
  ) : SleepTimerManager.Factory {

    override fun create(player: AudioPlayer): SleepTimerManager {
      return managerFactory(player)
    }
  }

  override val runningTimer = MutableStateFlow<RunningTimer?>(null)

  private var playbackTimer: PlaybackTimer? = null
  private var lastPlaybackTimer: PlaybackTimer? = null
  private var playbackTimerJob: Job? = null

  override fun onSessionStart() {
    if (sleepSettings.autoSleepTimerEnabled) {
      if (isNowAnAutoSleepZone()) {
        clearTimer()
        stopShakeDetector()

        val newTimer = when (val sleepTimer = sleepSettings.autoSleepTimer) {
          is SleepSettings.AutoSleepTimer.Epoch -> PlaybackTimer.Epoch(sleepTimer.millis, true)
          SleepSettings.AutoSleepTimer.EndOfChapter -> PlaybackTimer.EndOfChapter(true)
        }
        setTimer(newTimer)
      }
    }
  }

  private fun isNowAnAutoSleepZone(): Boolean {
    val now = fatherTime.now().time
    val start = sleepSettings.autoSleepStart
    val end = sleepSettings.autoSleepEnd

    if (end > start) {
      return now in start..end
    } else if (end < start) {
      return now in start..LocalTime.Midnight ||
        now in LocalTime.Zero..end
    } else {
      // This should never happen, and ideally we prevent
      // this on the UI level.
      return now == start
    }
  }

  override fun setTimer(timer: PlaybackTimer) {
    clearTimer()
    stopShakeDetector()
    playbackTimer = timer
    lastPlaybackTimer = timer
    runningTimer.value = RunningTimer(timer, fatherTime.nowInEpochMillis())
    startTimer(timer)

    if (sleepSettings.shakeToResetEnabled) {
      shakeDetector.start(
        sensitivity = sleepSettings.shakeSensitivity.asShakeSensitivity(),
        listener = { resetTimer() },
      )
    }
  }

  private fun resetTimer() {
    if (lastPlaybackTimer != null) {
      if (player.state.value == AudioPlayer.State.Paused) {
        player.playPause()
      }

      setTimer(lastPlaybackTimer!!)
    }
  }

  override fun clearTimer() {
    playbackTimerJob?.cancel()
    playbackTimerJob = null
    playbackTimer = null
    runningTimer.value = null
  }

  private fun endTimer() {
    // If the autoRewind and timer are enabled and the playbackTimer that just finished
    // then rewind by the configured amount
    if (
      playbackTimer?.isAutoSleepTimer == true &&
      sleepSettings.autoRewindEnabled &&
      sleepSettings.autoSleepTimerEnabled
    ) {
      val newTime = player.overallTime.value - sleepSettings.autoRewindAmount
      player.seekTo(newTime)
    }

    // Pause playback and clear the timer
    player.pause()
    clearTimer()

    // If we hit the end of the sleep timer delay for an amount of time,
    // then stop the shake detector allowing a brief period of time where the user
    // can shake to resume and reset the last known timer
    stopShakeDetector(withDelay = true)
  }

  private fun stopShakeDetector(withDelay: Boolean = false) {
    if (!withDelay || playbackTimer == null) {
      shakeDetector.stop()
      lastPlaybackTimer = null
    } else {
      applicationScope.launch {
        delay(30_000L) // 30s
        shakeDetector.stop()
        lastPlaybackTimer = null
      }
    }
  }

  override fun endOfChapter(): Boolean {
    if (playbackTimer is PlaybackTimer.EndOfChapter) {
      endTimer()
      return true
    }
    return false
  }

  private fun startTimer(timer: PlaybackTimer) {
    if (timer is PlaybackTimer.Epoch) {
      playbackTimerJob = applicationScope.async(dispatcherProvider.computation) {
        delay(timer.epochMillis)
        endTimer()
      }
    }
  }
}

fun SleepSettings.ShakeSensitivity.asShakeSensitivity(): ShakeSensitivity {
  return when (this) {
    SleepSettings.ShakeSensitivity.VeryLow -> ShakeSensitivity.VeryLow
    SleepSettings.ShakeSensitivity.Low -> ShakeSensitivity.Low
    SleepSettings.ShakeSensitivity.Medium -> ShakeSensitivity.Medium
    SleepSettings.ShakeSensitivity.High -> ShakeSensitivity.High
    SleepSettings.ShakeSensitivity.VeryHigh -> ShakeSensitivity.VeryHigh
  }
}

val LocalTime.Companion.Midnight: LocalTime
  get() = LocalTime(23, 59, 59)

val LocalTime.Companion.Zero: LocalTime
  get() = LocalTime(0, 0, 0)

// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.sleep

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.Cork
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
import kotlinx.coroutines.withContext
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
  private var countdownJob: Job? = null
  private var playbackStateJob: Job? = null
  private var fadeJob: Job? = null
  private var shakeDetectorJob: Job? = null

  /**
   * Time left on the running [PlaybackTimer.Epoch] timer. Exact while the countdown is paused;
   * while it runs, the portion elapsed since [countdownResumedAt] still has to be subtracted.
   */
  private var remainingMillis = 0L
  private var countdownResumedAt: Long? = null

  override fun onSessionStart() {
    if (sleepSettings.autoSleepTimerEnabled && playbackTimer == null) {
      if (isNowAnAutoSleepZone()) {
        ibark { "Starting auto sleep timer" }
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
    ibark { "setTimer($timer)" }
    clearTimerInternal()
    // A timer set (or reset via shake) mid-fade supersedes the fade, so keep listening
    cancelFade()
    playbackTimer = timer
    lastPlaybackTimer = timer
    startTimer(timer)

    if (sleepSettings.shakeToResetEnabled && !shakeDetector.isRunning) {
      ibark { "Shake to reset enabled, starting shake detector" }
      shakeDetector.start(
        sensitivity = sleepSettings.shakeSensitivity.asShakeSensitivity(),
        listener = {
          dbark { "<~> Shake Detected!" }
          resetTimer()
        },
      )
    } else if (sleepSettings.shakeToResetEnabled && shakeDetector.isRunning) {
      dbark { "Shake detector already running, skipping" }
    } else if (!sleepSettings.shakeToResetEnabled && shakeDetector.isRunning) {
      dbark { "Shake detector running, and not enabled. Stopping" }
      stopShakeDetector()
    }
  }

  private fun resetTimer() {
    dbark { "resetTimer(lastTimer=$lastPlaybackTimer)" }
    if (lastPlaybackTimer != null) {
      // Cancel our shake detector time out job so that it doesn't kill
      // the shake detection while the timer is re-activated
      shakeDetectorJob?.cancel()
      shakeDetectorJob = null

      // Re-active the last playback timer
      setTimer(lastPlaybackTimer!!)

      // If the player is paused (i.e. the previous timer finished) be sure to resume
      // playback
      if (player.state.value == AudioPlayer.State.Paused) {
        player.playPause()
      }
    }
  }

  override fun clearTimer() {
    ibark { "clearTimer(current=$playbackTimer)" }
    clearTimerInternal()
    cancelFade()
    stopShakeDetector()
  }

  private fun clearTimerInternal() {
    dbark { "clearTimerInternal()" }
    countdownJob?.cancel()
    countdownJob = null
    playbackStateJob?.cancel()
    playbackStateJob = null
    countdownResumedAt = null
    remainingMillis = 0L
    playbackTimer = null
    runningTimer.value = null
  }

  private fun cancelFade() {
    fadeJob?.let {
      dbark { "Cancelling in-progress fade" }
      it.cancel()
    }
    fadeJob = null
  }

  private fun endTimer() {
    val endedTimer = playbackTimer ?: return
    ibark { "endTimer($endedTimer)" }

    // If the autoRewind and timer are enabled and the playbackTimer that just finished
    // then rewind by the configured amount. Captured now because the timer is cleared below,
    // long before a fade finishes.
    val onPauseComplete: () -> Unit = {
      if (
        endedTimer.isAutoSleepTimer &&
        sleepSettings.autoRewindEnabled &&
        sleepSettings.autoSleepTimerEnabled
      ) {
        val newTime = player.overallTime.value - sleepSettings.autoRewindAmount
        player.seekTo(newTime)
        dbark { "Auto-sleep timer ended with rewind enabled, seeking to $newTime" }
      }
    }

    // Clear before pausing so the countdown's own playback-state observer doesn't react to the pause
    clearTimerInternal()

    if (endedTimer is PlaybackTimer.EndOfChapter) {
      player.pause()
      onPauseComplete()
    } else {
      // Fade playback out over the configured duration, then pause
      cancelFade()
      val fade = player.fadeToPause(duration = sleepSettings.fadeOutDuration)
      fadeJob = fade
      fade.invokeOnCompletion { cause ->
        // Only a fade that ran to completion actually put the listener to sleep
        if (cause == null) onPauseComplete()
      }
      cancelFadeIfPlaybackResumes(fade)
    }

    // If we hit the end of the sleep timer delay for an amount of time,
    // then stop the shake detector allowing a brief period of time where the user
    // can shake to resume and reset the last known timer
    stopShakeDetector(withDelay = true)
  }

  private fun stopShakeDetector(withDelay: Boolean = false) {
    if (!shakeDetector.isRunning) {
      lastPlaybackTimer = null
      dbark { "ShakeDetector is not running, clearing last playback timer" }
    } else if (!withDelay || lastPlaybackTimer == null) {
      dbark { "Stopping ShakeDetector without delay" }
      shakeDetector.stop()
      lastPlaybackTimer = null
    } else {
      dbark { "--> Launching delayed shake detector stop" }
      shakeDetectorJob?.cancel()
      shakeDetectorJob = applicationScope.launch {
        delay(30_000L) // 30s
        shakeDetector.stop()
        lastPlaybackTimer = null
        dbark { "<-- ShakeDetector stopped!" }
      }
    }
  }

  override fun endOfChapter(): Boolean {
    if (playbackTimer is PlaybackTimer.EndOfChapter) {
      dbark { "endOfChapter($playbackTimer)" }
      endTimer()
      return true
    }
    return false
  }

  /**
   * A listener who pauses and then presses play while the fade is still running wants to keep
   * listening: cancel the fade so the volume comes back instead of it pausing again moments later.
   * Only a Paused -> Playing transition counts, so buffering hiccups mid-fade are ignored.
   */
  private fun cancelFadeIfPlaybackResumes(fade: Job) {
    val watcher = applicationScope.launch(dispatcherProvider.main) {
      var previous = player.state.value
      player.state.collect { state ->
        if (previous == AudioPlayer.State.Paused && state == AudioPlayer.State.Playing) {
          ibark { "Playback resumed mid-fade, cancelling the fade" }
          fade.cancel()
        }
        previous = state
      }
    }
    fade.invokeOnCompletion {
      watcher.cancel()
      if (fadeJob === fade) fadeJob = null
    }
  }

  private fun startTimer(timer: PlaybackTimer) {
    if (timer !is PlaybackTimer.Epoch) {
      runningTimer.value = RunningTimer(timer, fatherTime.nowInEpochMillis(), sleepSettings.shakeToResetEnabled)
      return
    }

    remainingMillis = timer.epochMillis
    countdownResumedAt = null

    // The countdown only counts listening time: it runs while audio is playing, freezes when playback
    // pauses (or is still preparing) and picks back up on resume.
    playbackStateJob = applicationScope.launch(dispatcherProvider.main) {
      player.state.collect { state ->
        if (state == AudioPlayer.State.Playing) resumeCountdown(timer) else pauseCountdown(timer)
      }
    }
  }

  private fun resumeCountdown(timer: PlaybackTimer.Epoch) {
    if (countdownResumedAt != null) return
    val now = fatherTime.nowInEpochMillis()
    countdownResumedAt = now
    publishRunningTimer(timer, reference = now, pausedAt = null)

    val remaining = remainingMillis
    countdownJob?.cancel()
    countdownJob = applicationScope.async(dispatcherProvider.computation) {
      dbark { "--> Starting Epoch Timer (${remaining}ms remaining)" }
      delay(remaining)
      dbark { "<-- Epoch Timer Ended" }
      withContext(dispatcherProvider.main) {
        endTimer()
      }
    }
  }

  private fun pauseCountdown(timer: PlaybackTimer.Epoch) {
    val now = fatherTime.nowInEpochMillis()
    val resumedAt = countdownResumedAt
    if (resumedAt != null) {
      dbark { "Playback paused, freezing sleep timer" }
      remainingMillis = (remainingMillis - (now - resumedAt)).coerceAtLeast(0L)
      countdownResumedAt = null
      countdownJob?.cancel()
      countdownJob = null
    }
    publishRunningTimer(timer, reference = now, pausedAt = now)
  }

  /**
   * Consumers derive time left as `epochMillis - (reference - startedAt)`, so [RunningTimer.startedAt]
   * is back-dated by whatever has already elapsed rather than being the instant the timer was set.
   */
  private fun publishRunningTimer(timer: PlaybackTimer.Epoch, reference: Long, pausedAt: Long?) {
    runningTimer.value = RunningTimer(
      timer = timer,
      startedAt = reference - (timer.epochMillis - remainingMillis),
      isShakeToRestartEnabled = sleepSettings.shakeToResetEnabled,
      pausedAt = pausedAt,
    )
  }

  companion object : Cork {
    override val tag: String = "CoroutineSleepTimerManager"
    override val enabled: Boolean = true
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

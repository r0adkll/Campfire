// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

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
  /**
   * When non-null the countdown is frozen at this instant because playback is paused. It resumes,
   * with [startedAt] shifted forward by the time spent paused, once playback continues.
   */
  val pausedAt: Long? = null,
) {

  val isPaused: Boolean
    get() = pausedAt != null

  /**
   * Milliseconds left on an [PlaybackTimer.Epoch] timer as of [now], or null for timers without a
   * countdown. Frozen while [isPaused].
   */
  fun remainingMillis(now: Long): Long? {
    val epoch = timer as? PlaybackTimer.Epoch ?: return null
    val reference = pausedAt ?: now
    return (epoch.epochMillis - (reference - startedAt)).coerceAtLeast(0L)
  }
}

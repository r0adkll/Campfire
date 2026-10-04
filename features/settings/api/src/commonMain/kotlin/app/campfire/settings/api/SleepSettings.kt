// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import app.campfire.core.settings.EnumSetting
import app.campfire.core.settings.EnumSettingProvider
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalTime

interface SleepSettings {

  fun setLastSetSleepTimer(value: Duration)
  fun observeLastSetSleepTimer(): Flow<Duration>

  fun setShakeToResetEnabled(value: Boolean)
  fun observeShakeToResetEnabled(): Flow<Boolean>

  fun setShakeSensitivity(value: ShakeSensitivity)
  fun observeShakeSensitivity(): Flow<ShakeSensitivity>

  fun setAutoSleepTimerEnabled(value: Boolean)
  fun observeAutoSleepTimerEnabled(): Flow<Boolean>

  fun setAutoSleepStart(value: LocalTime)
  fun observeAutoSleepStart(): Flow<LocalTime>

  fun setAutoSleepEnd(value: LocalTime)
  fun observeAutoSleepEnd(): Flow<LocalTime>

  fun setAutoSleepTimer(value: AutoSleepTimer)
  fun observeAutoSleepTimer(): Flow<AutoSleepTimer>

  fun setAutoRewindEnabled(value: Boolean)
  fun observeAutoRewindEnabled(): Flow<Boolean>

  fun setAutoRewindAmount(value: Duration)
  fun observeAutoRewindAmount(): Flow<Duration>

  /**
   * How long the volume fades out before a sleep timer pauses playback. [Duration.ZERO] pauses immediately.
   */
  fun setFadeOutDuration(value: Duration)
  fun observeFadeOutDuration(): Flow<Duration>

  sealed class AutoSleepTimer {
    data class Epoch(val millis: Long) : AutoSleepTimer()
    data object EndOfChapter : AutoSleepTimer()

    companion object {
      val Default get() = Epoch(15.minutes.inWholeMilliseconds)
    }
  }

  enum class ShakeSensitivity(override val storageKey: String) : EnumSetting {
    VeryLow("very_low"),
    Low("low"),
    Medium("medium"),
    High("high"),
    VeryHigh("very_high"),
    ;

    companion object : EnumSettingProvider<ShakeSensitivity> {
      val Default get() = Medium

      override fun fromStorageKey(key: String?): ShakeSensitivity {
        return entries.find { it.storageKey == key } ?: Medium
      }
    }
  }

  companion object {
    const val DEFAULT_SHAKE_TO_RESET_ENABLED = false
    const val DEFAULT_AUTO_SLEEP_TIMER_ENABLED = false
    const val DEFAULT_AUTO_REWIND_ENABLED = false
    val DefaultLastSetSleepTimer: Duration get() = 10.minutes
    val DefaultAutoRewindAmount: Duration get() = 5.minutes
    val DefaultAutoSleepStart: LocalTime get() = LocalTime(22, 0)
    val DefaultAutoSleepEnd: LocalTime get() = LocalTime(6, 0)
    val DefaultFadeOutDuration: Duration get() = 5.seconds
    val FadeOutDurationRange: ClosedRange<Duration> get() = Duration.ZERO..60.seconds
  }
}

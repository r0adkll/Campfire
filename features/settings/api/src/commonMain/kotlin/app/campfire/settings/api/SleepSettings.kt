// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import app.campfire.core.settings.EnumSetting
import app.campfire.core.settings.EnumSettingProvider
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalTime

interface SleepSettings {

  val lastSetSleepTimer: Duration
  fun setLastSetSleepTimer(value: Duration)
  fun observeLastSetSleepTimer(): StateFlow<Duration>

  val shakeToResetEnabled: Boolean
  fun setShakeToResetEnabled(value: Boolean)
  fun observeShakeToResetEnabled(): StateFlow<Boolean>

  val shakeSensitivity: ShakeSensitivity
  fun setShakeSensitivity(value: ShakeSensitivity)
  fun observeShakeSensitivity(): StateFlow<ShakeSensitivity>

  val autoSleepTimerEnabled: Boolean
  fun setAutoSleepTimerEnabled(value: Boolean)
  fun observeAutoSleepTimerEnabled(): StateFlow<Boolean>

  val autoSleepStart: LocalTime
  fun setAutoSleepStart(value: LocalTime)
  fun observeAutoSleepStart(): StateFlow<LocalTime>

  val autoSleepEnd: LocalTime
  fun setAutoSleepEnd(value: LocalTime)
  fun observeAutoSleepEnd(): StateFlow<LocalTime>

  val autoSleepTimer: AutoSleepTimer
  fun setAutoSleepTimer(value: AutoSleepTimer)
  fun observeAutoSleepTimer(): StateFlow<AutoSleepTimer>

  val autoRewindEnabled: Boolean
  fun setAutoRewindEnabled(value: Boolean)
  fun observeAutoRewindEnabled(): StateFlow<Boolean>

  val autoRewindAmount: Duration
  fun setAutoRewindAmount(value: Duration)
  fun observeAutoRewindAmount(): StateFlow<Duration>

  /**
   * How long the volume fades out before a sleep timer pauses playback. [Duration.ZERO] pauses immediately.
   */
  val fadeOutDuration: Duration
  fun setFadeOutDuration(value: Duration)
  fun observeFadeOutDuration(): StateFlow<Duration>

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
    val DefaultFadeOutDuration: Duration get() = 5.seconds
    val FadeOutDurationRange: ClosedRange<Duration> get() = Duration.ZERO..60.seconds
  }
}

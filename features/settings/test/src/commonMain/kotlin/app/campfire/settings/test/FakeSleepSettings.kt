// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.test

import app.campfire.settings.api.SleepSettings
import app.campfire.settings.api.SleepSettings.AutoSleepTimer
import app.campfire.settings.api.SleepSettings.ShakeSensitivity
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalTime

/**
 * An in-memory [SleepSettings] fake backed by [MutableStateFlow]s for use in tests.
 */
class FakeSleepSettings : SleepSettings {

  private val _lastSetSleepTimer = MutableStateFlow<Duration>(10.minutes)
  val lastSetSleepTimer: Duration get() = _lastSetSleepTimer.value
  override fun setLastSetSleepTimer(value: Duration) {
    _lastSetSleepTimer.value = value
  }
  override fun observeLastSetSleepTimer(): StateFlow<Duration> = _lastSetSleepTimer.asStateFlow()

  private val _shakeToResetEnabled = MutableStateFlow<Boolean>(false)
  val shakeToResetEnabled: Boolean get() = _shakeToResetEnabled.value
  override fun setShakeToResetEnabled(value: Boolean) {
    _shakeToResetEnabled.value = value
  }
  override fun observeShakeToResetEnabled(): StateFlow<Boolean> = _shakeToResetEnabled.asStateFlow()

  private val _shakeSensitivity = MutableStateFlow<ShakeSensitivity>(ShakeSensitivity.Default)
  val shakeSensitivity: ShakeSensitivity get() = _shakeSensitivity.value
  override fun setShakeSensitivity(value: ShakeSensitivity) {
    _shakeSensitivity.value = value
  }
  override fun observeShakeSensitivity(): StateFlow<ShakeSensitivity> = _shakeSensitivity.asStateFlow()

  private val _autoSleepTimerEnabled = MutableStateFlow<Boolean>(false)
  val autoSleepTimerEnabled: Boolean get() = _autoSleepTimerEnabled.value
  override fun setAutoSleepTimerEnabled(value: Boolean) {
    _autoSleepTimerEnabled.value = value
  }
  override fun observeAutoSleepTimerEnabled(): StateFlow<Boolean> = _autoSleepTimerEnabled.asStateFlow()

  private val _autoSleepStart = MutableStateFlow<LocalTime>(LocalTime(22, 0))
  val autoSleepStart: LocalTime get() = _autoSleepStart.value
  override fun setAutoSleepStart(value: LocalTime) {
    _autoSleepStart.value = value
  }
  override fun observeAutoSleepStart(): StateFlow<LocalTime> = _autoSleepStart.asStateFlow()

  private val _autoSleepEnd = MutableStateFlow<LocalTime>(LocalTime(6, 0))
  val autoSleepEnd: LocalTime get() = _autoSleepEnd.value
  override fun setAutoSleepEnd(value: LocalTime) {
    _autoSleepEnd.value = value
  }
  override fun observeAutoSleepEnd(): StateFlow<LocalTime> = _autoSleepEnd.asStateFlow()

  private val _autoSleepTimer = MutableStateFlow<AutoSleepTimer>(AutoSleepTimer.Default)
  val autoSleepTimer: AutoSleepTimer get() = _autoSleepTimer.value
  override fun setAutoSleepTimer(value: AutoSleepTimer) {
    _autoSleepTimer.value = value
  }
  override fun observeAutoSleepTimer(): StateFlow<AutoSleepTimer> = _autoSleepTimer.asStateFlow()

  private val _autoRewindEnabled = MutableStateFlow<Boolean>(false)
  val autoRewindEnabled: Boolean get() = _autoRewindEnabled.value
  override fun setAutoRewindEnabled(value: Boolean) {
    _autoRewindEnabled.value = value
  }
  override fun observeAutoRewindEnabled(): StateFlow<Boolean> = _autoRewindEnabled.asStateFlow()

  private val _autoRewindAmount = MutableStateFlow<Duration>(5.minutes)
  val autoRewindAmount: Duration get() = _autoRewindAmount.value
  override fun setAutoRewindAmount(value: Duration) {
    _autoRewindAmount.value = value
  }
  override fun observeAutoRewindAmount(): StateFlow<Duration> = _autoRewindAmount.asStateFlow()

  private val _fadeOutDuration = MutableStateFlow<Duration>(SleepSettings.DefaultFadeOutDuration)
  val fadeOutDuration: Duration get() = _fadeOutDuration.value
  override fun setFadeOutDuration(value: Duration) {
    _fadeOutDuration.value = value
  }
  override fun observeFadeOutDuration(): StateFlow<Duration> = _fadeOutDuration.asStateFlow()
}

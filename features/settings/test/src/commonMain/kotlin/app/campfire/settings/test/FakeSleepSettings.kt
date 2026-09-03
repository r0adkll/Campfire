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
 * A simple in-memory [SleepSettings] fake backed by [MutableStateFlow]s for use in tests.
 */
class FakeSleepSettings : SleepSettings {

  private val _lastSetSleepTimer = MutableStateFlow(10.minutes)
  override var lastSetSleepTimer: Duration
    get() = _lastSetSleepTimer.value
    set(value) { _lastSetSleepTimer.value = value }
  override fun observeLastSetSleepTimer(): StateFlow<Duration> = _lastSetSleepTimer.asStateFlow()

  private val _shakeToResetEnabled = MutableStateFlow(false)
  override var shakeToResetEnabled: Boolean
    get() = _shakeToResetEnabled.value
    set(value) { _shakeToResetEnabled.value = value }
  override fun observeShakeToResetEnabled(): StateFlow<Boolean> = _shakeToResetEnabled.asStateFlow()

  private val _shakeSensitivity = MutableStateFlow(ShakeSensitivity.Default)
  override var shakeSensitivity: ShakeSensitivity
    get() = _shakeSensitivity.value
    set(value) { _shakeSensitivity.value = value }
  override fun observeShakeSensitivity(): StateFlow<ShakeSensitivity> = _shakeSensitivity.asStateFlow()

  private val _autoSleepTimerEnabled = MutableStateFlow(false)
  override var autoSleepTimerEnabled: Boolean
    get() = _autoSleepTimerEnabled.value
    set(value) { _autoSleepTimerEnabled.value = value }
  override fun observeAutoSleepTimerEnabled(): StateFlow<Boolean> = _autoSleepTimerEnabled.asStateFlow()

  private val _autoSleepStart = MutableStateFlow(LocalTime(22, 0))
  override var autoSleepStart: LocalTime
    get() = _autoSleepStart.value
    set(value) { _autoSleepStart.value = value }
  override fun observeAutoSleepStart(): StateFlow<LocalTime> = _autoSleepStart.asStateFlow()

  private val _autoSleepEnd = MutableStateFlow(LocalTime(6, 0))
  override var autoSleepEnd: LocalTime
    get() = _autoSleepEnd.value
    set(value) { _autoSleepEnd.value = value }
  override fun observeAutoSleepEnd(): StateFlow<LocalTime> = _autoSleepEnd.asStateFlow()

  private val _autoSleepTimer = MutableStateFlow<AutoSleepTimer>(AutoSleepTimer.Default)
  override var autoSleepTimer: AutoSleepTimer
    get() = _autoSleepTimer.value
    set(value) { _autoSleepTimer.value = value }
  override fun observeAutoSleepTimer(): StateFlow<AutoSleepTimer> = _autoSleepTimer.asStateFlow()

  private val _autoRewindEnabled = MutableStateFlow(false)
  override var autoRewindEnabled: Boolean
    get() = _autoRewindEnabled.value
    set(value) { _autoRewindEnabled.value = value }
  override fun observeAutoRewindEnabled(): StateFlow<Boolean> = _autoRewindEnabled.asStateFlow()

  private val _autoRewindAmount = MutableStateFlow(5.minutes)
  override var autoRewindAmount: Duration
    get() = _autoRewindAmount.value
    set(value) { _autoRewindAmount.value = value }
  override fun observeAutoRewindAmount(): StateFlow<Duration> = _autoRewindAmount.asStateFlow()

  private val _fadeOutDuration = MutableStateFlow(SleepSettings.DefaultFadeOutDuration)
  override var fadeOutDuration: Duration
    get() = _fadeOutDuration.value
    set(value) { _fadeOutDuration.value = value }
  override fun observeFadeOutDuration(): StateFlow<Duration> = _fadeOutDuration.asStateFlow()
}

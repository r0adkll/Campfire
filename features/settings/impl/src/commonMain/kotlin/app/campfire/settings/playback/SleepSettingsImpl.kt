// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.playback

import app.campfire.core.di.AppScope
import app.campfire.settings.api.SleepSettings
import app.campfire.settings.api.SleepSettings.AutoSleepTimer
import app.campfire.settings.api.SleepSettings.ShakeSensitivity
import app.campfire.settings.store.AppSettings
import app.campfire.settings.store.SettingsStore
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalTime

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<SleepSettings>())
@Inject
class SleepSettingsImpl(
  override val store: SettingsStore,
) : SleepSettings, AppSettings() {

  private val lastSetSleepTimerProperty =
    durationSetting(KEY_LAST_SET_SLEEP_TIMER, SleepSettings.DefaultLastSetSleepTimer)
  override fun setLastSetSleepTimer(value: Duration) = lastSetSleepTimerProperty.set(value)
  override fun observeLastSetSleepTimer(): Flow<Duration> = lastSetSleepTimerProperty.observe()

  private val shakeToResetEnabledProperty =
    booleanSetting(KEY_SHAKE_TO_RESET, SleepSettings.DEFAULT_SHAKE_TO_RESET_ENABLED)
  override fun setShakeToResetEnabled(value: Boolean) = shakeToResetEnabledProperty.set(value)
  override fun observeShakeToResetEnabled(): Flow<Boolean> = shakeToResetEnabledProperty.observe()

  private val shakeSensitivityProperty = enumSetting(KEY_SHAKE_SENSITIVITY, ShakeSensitivity)
  override fun setShakeSensitivity(value: ShakeSensitivity) = shakeSensitivityProperty.set(value)
  override fun observeShakeSensitivity(): Flow<ShakeSensitivity> = shakeSensitivityProperty.observe()

  private val autoSleepTimerEnabledProperty = booleanSetting(
    KEY_AUTO_SLEEP_TIMER_ENABLED,
    SleepSettings.DEFAULT_AUTO_SLEEP_TIMER_ENABLED,
  )
  override fun setAutoSleepTimerEnabled(value: Boolean) = autoSleepTimerEnabledProperty.set(value)
  override fun observeAutoSleepTimerEnabled(): Flow<Boolean> = autoSleepTimerEnabledProperty.observe()

  private val autoSleepStartProperty = localTimeSetting(KEY_AUTO_SLEEP_START, SleepSettings.DefaultAutoSleepStart)
  override fun setAutoSleepStart(value: LocalTime) = autoSleepStartProperty.set(value)
  override fun observeAutoSleepStart(): Flow<LocalTime> = autoSleepStartProperty.observe()

  private val autoSleepEndProperty = localTimeSetting(KEY_AUTO_SLEEP_END, SleepSettings.DefaultAutoSleepEnd)
  override fun setAutoSleepEnd(value: LocalTime) = autoSleepEndProperty.set(value)
  override fun observeAutoSleepEnd(): Flow<LocalTime> = autoSleepEndProperty.observe()

  private val timerTypeSeparator = ";;"
  private val timerFromString: (String) -> AutoSleepTimer = { value ->
    val parts = value.split(timerTypeSeparator)
    check(parts.size == 2)
    when (parts[0]) {
      "epoch" -> AutoSleepTimer.Epoch(parts[1].toLong())
      "end_of_chapter" -> AutoSleepTimer.EndOfChapter
      else -> error("Unknown timer type: ${parts[0]}")
    }
  }

  private val timerToString: (AutoSleepTimer) -> String = { timer ->
    when (timer) {
      AutoSleepTimer.EndOfChapter -> "end_of_chapter$timerTypeSeparator--"
      is AutoSleepTimer.Epoch -> "epoch${timerTypeSeparator}${timer.millis}"
    }
  }

  private val autoSleepTimerProperty = customSetting(
    key = KEY_AUTO_SLEEP_TIMER,
    defaultValue = AutoSleepTimer.Default,
    getter = timerFromString,
    setter = timerToString,
  )
  override fun setAutoSleepTimer(value: AutoSleepTimer) = autoSleepTimerProperty.set(value)
  override fun observeAutoSleepTimer(): Flow<AutoSleepTimer> = autoSleepTimerProperty.observe()

  private val autoRewindEnabledProperty =
    booleanSetting(KEY_AUTO_REWIND_ENABLED, SleepSettings.DEFAULT_AUTO_REWIND_ENABLED)
  override fun setAutoRewindEnabled(value: Boolean) = autoRewindEnabledProperty.set(value)
  override fun observeAutoRewindEnabled(): Flow<Boolean> = autoRewindEnabledProperty.observe()

  private val autoRewindAmountProperty = durationSetting(KEY_AUTO_REWIND_AMOUNT, SleepSettings.DefaultAutoRewindAmount)
  override fun setAutoRewindAmount(value: Duration) = autoRewindAmountProperty.set(value)
  override fun observeAutoRewindAmount(): Flow<Duration> = autoRewindAmountProperty.observe()

  private val fadeOutDurationProperty = durationSetting(KEY_FADE_OUT_DURATION, SleepSettings.DefaultFadeOutDuration)
  override fun setFadeOutDuration(value: Duration) = fadeOutDurationProperty.set(value)
  override fun observeFadeOutDuration(): Flow<Duration> = fadeOutDurationProperty.observe()
}

internal const val KEY_LAST_SET_SLEEP_TIMER = "pref_last_set_sleep_timer"
internal const val KEY_SHAKE_TO_RESET = "pref_sleep_shake_to_reset"
internal const val KEY_SHAKE_SENSITIVITY = "pref_sleep_shake_sensitivity"
internal const val KEY_AUTO_SLEEP_TIMER_ENABLED = "pref_sleep_auto_timer_enabled"
internal const val KEY_AUTO_SLEEP_START = "pref_sleep_auto_timer_start"
internal const val KEY_AUTO_SLEEP_END = "pref_sleep_auto_timer_end"
internal const val KEY_AUTO_SLEEP_TIMER = "pref_auto_sleep_timer"
internal const val KEY_AUTO_REWIND_ENABLED = "pref_auto_rewind_enabled"
internal const val KEY_AUTO_REWIND_AMOUNT = "pref_auto_rewind_amount"
internal const val KEY_FADE_OUT_DURATION = "pref_sleep_fade_out_duration"

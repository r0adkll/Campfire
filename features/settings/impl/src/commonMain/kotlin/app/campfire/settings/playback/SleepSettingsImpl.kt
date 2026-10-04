// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.playback

import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.settings.api.SleepSettings
import app.campfire.settings.api.SleepSettings.AutoSleepTimer
import app.campfire.settings.api.SleepSettings.ShakeSensitivity
import app.campfire.settings.store.AppSettings
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalTime

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<SleepSettings>())
@Inject
class SleepSettingsImpl(
  override val settings: ObservableSettings,
  @ForScope(AppScope::class) override val scope: CoroutineScope,
) : SleepSettings, AppSettings() {

  private val lastSetSleepTimerProperty =
    durationSetting(KEY_LAST_SET_SLEEP_TIMER, SleepSettings.DefaultLastSetSleepTimer)
  override val lastSetSleepTimer: Duration by lastSetSleepTimerProperty
  override fun setLastSetSleepTimer(value: Duration) = lastSetSleepTimerProperty.set(value)
  override fun observeLastSetSleepTimer(): StateFlow<Duration> = lastSetSleepTimerProperty.observe()

  private val shakeToResetEnabledProperty =
    booleanSetting(KEY_SHAKE_TO_RESET, SleepSettings.DEFAULT_SHAKE_TO_RESET_ENABLED)
  override val shakeToResetEnabled: Boolean by shakeToResetEnabledProperty
  override fun setShakeToResetEnabled(value: Boolean) = shakeToResetEnabledProperty.set(value)
  override fun observeShakeToResetEnabled(): StateFlow<Boolean> = shakeToResetEnabledProperty.observe()

  private val shakeSensitivityProperty = enumSetting(KEY_SHAKE_SENSITIVITY, ShakeSensitivity)
  override val shakeSensitivity: ShakeSensitivity by shakeSensitivityProperty
  override fun setShakeSensitivity(value: ShakeSensitivity) = shakeSensitivityProperty.set(value)
  override fun observeShakeSensitivity(): StateFlow<ShakeSensitivity> = shakeSensitivityProperty.observe()

  private val autoSleepTimerEnabledProperty = booleanSetting(
    KEY_AUTO_SLEEP_TIMER_ENABLED,
    SleepSettings.DEFAULT_AUTO_SLEEP_TIMER_ENABLED,
  )
  override val autoSleepTimerEnabled: Boolean by autoSleepTimerEnabledProperty
  override fun setAutoSleepTimerEnabled(value: Boolean) = autoSleepTimerEnabledProperty.set(value)
  override fun observeAutoSleepTimerEnabled(): StateFlow<Boolean> = autoSleepTimerEnabledProperty.observe()

  private val autoSleepStartProperty = localTimeSetting(KEY_AUTO_SLEEP_START, SleepSettings.DefaultAutoSleepStart)
  override val autoSleepStart: LocalTime by autoSleepStartProperty
  override fun setAutoSleepStart(value: LocalTime) = autoSleepStartProperty.set(value)
  override fun observeAutoSleepStart(): StateFlow<LocalTime> = autoSleepStartProperty.observe()

  private val autoSleepEndProperty = localTimeSetting(KEY_AUTO_SLEEP_END, SleepSettings.DefaultAutoSleepEnd)
  override val autoSleepEnd: LocalTime by autoSleepEndProperty
  override fun setAutoSleepEnd(value: LocalTime) = autoSleepEndProperty.set(value)
  override fun observeAutoSleepEnd(): StateFlow<LocalTime> = autoSleepEndProperty.observe()

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
  override val autoSleepTimer: AutoSleepTimer by autoSleepTimerProperty
  override fun setAutoSleepTimer(value: AutoSleepTimer) = autoSleepTimerProperty.set(value)
  override fun observeAutoSleepTimer(): StateFlow<AutoSleepTimer> = autoSleepTimerProperty.observe()

  private val autoRewindEnabledProperty =
    booleanSetting(KEY_AUTO_REWIND_ENABLED, SleepSettings.DEFAULT_AUTO_REWIND_ENABLED)
  override val autoRewindEnabled: Boolean by autoRewindEnabledProperty
  override fun setAutoRewindEnabled(value: Boolean) = autoRewindEnabledProperty.set(value)
  override fun observeAutoRewindEnabled(): StateFlow<Boolean> = autoRewindEnabledProperty.observe()

  private val autoRewindAmountProperty = durationSetting(KEY_AUTO_REWIND_AMOUNT, SleepSettings.DefaultAutoRewindAmount)
  override val autoRewindAmount: Duration by autoRewindAmountProperty
  override fun setAutoRewindAmount(value: Duration) = autoRewindAmountProperty.set(value)
  override fun observeAutoRewindAmount(): StateFlow<Duration> = autoRewindAmountProperty.observe()

  private val fadeOutDurationProperty = durationSetting(KEY_FADE_OUT_DURATION, SleepSettings.DefaultFadeOutDuration)
  override val fadeOutDuration: Duration by fadeOutDurationProperty
  override fun setFadeOutDuration(value: Duration) = fadeOutDurationProperty.set(value)
  override fun observeFadeOutDuration(): StateFlow<Duration> = fadeOutDurationProperty.observe()
}

private const val KEY_LAST_SET_SLEEP_TIMER = "pref_last_set_sleep_timer"
private const val KEY_SHAKE_TO_RESET = "pref_sleep_shake_to_reset"
private const val KEY_SHAKE_SENSITIVITY = "pref_sleep_shake_sensitivity"
private const val KEY_AUTO_SLEEP_TIMER_ENABLED = "pref_sleep_auto_timer_enabled"
private const val KEY_AUTO_SLEEP_START = "pref_sleep_auto_timer_start"
private const val KEY_AUTO_SLEEP_END = "pref_sleep_auto_timer_end"
private const val KEY_AUTO_SLEEP_TIMER = "pref_auto_sleep_timer"
private const val KEY_AUTO_REWIND_ENABLED = "pref_auto_rewind_enabled"
private const val KEY_AUTO_REWIND_AMOUNT = "pref_auto_rewind_amount"
private const val KEY_FADE_OUT_DURATION = "pref_sleep_fade_out_duration"

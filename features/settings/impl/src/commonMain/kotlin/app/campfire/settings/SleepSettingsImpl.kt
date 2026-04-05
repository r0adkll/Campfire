package app.campfire.settings

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.qualifier.ForScope
import app.campfire.settings.api.SleepSettings
import app.campfire.settings.api.SleepSettings.AutoSleepTimer
import app.campfire.settings.api.SleepSettings.ShakeSensitivity
import com.r0adkll.kimchi.annotations.ContributesBinding
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalTime
import me.tatarka.inject.annotations.Inject

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = SleepSettings::class)
@Inject
class SleepSettingsImpl(
  override val settings: ObservableSettings,
  @ForScope(AppScope::class) override val scope: CoroutineScope,
) : SleepSettings, AppSettings() {

  private val lastSetSleepTimerProperty = durationSetting(KEY_LAST_SET_SLEEP_TIMER, 10.minutes)
  override var lastSetSleepTimer: Duration by lastSetSleepTimerProperty
  override fun observeLastSetSleepTimer(): StateFlow<Duration> = lastSetSleepTimerProperty.observe()

  private val shakeToResetEnabledProperty = booleanSetting(KEY_SHAKE_TO_RESET, DefaultShakeToResetEnabled)
  override var shakeToResetEnabled: Boolean by shakeToResetEnabledProperty
  override fun observeShakeToResetEnabled(): StateFlow<Boolean> = shakeToResetEnabledProperty.observe()

  private val shakeSensitivityProperty = enumSetting(KEY_SHAKE_SENSITIVITY, ShakeSensitivity)
  override var shakeSensitivity: ShakeSensitivity by shakeSensitivityProperty
  override fun observeShakeSensitivity(): StateFlow<ShakeSensitivity> = shakeSensitivityProperty.observe()

  private val autoSleepTimerEnabledProperty = booleanSetting(
    KEY_AUTO_SLEEP_TIMER_ENABLED,
    DefaultAutoSleepTimerEnabled,
  )
  override var autoSleepTimerEnabled: Boolean by autoSleepTimerEnabledProperty
  override fun observeAutoSleepTimerEnabled(): StateFlow<Boolean> = autoSleepTimerEnabledProperty.observe()

  // 10:00 PM
  private val defaultStartTime: LocalTime
    get() = LocalTime(22, 0)

  private val autoSleepStartProperty = localTimeSetting(KEY_AUTO_SLEEP_START, defaultStartTime)
  override var autoSleepStart: LocalTime by autoSleepStartProperty
  override fun observeAutoSleepStart(): StateFlow<LocalTime> = autoSleepStartProperty.observe()

  // 6:00 AM
  private val defaultEndTime: LocalTime
    get() = LocalTime(6, 0)

  private val autoSleepEndProperty = localTimeSetting(KEY_AUTO_SLEEP_END, defaultEndTime)
  override var autoSleepEnd: LocalTime by autoSleepEndProperty
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
  override var autoSleepTimer: AutoSleepTimer by autoSleepTimerProperty
  override fun observeAutoSleepTimer(): StateFlow<AutoSleepTimer> = autoSleepTimerProperty.observe()

  private val autoRewindEnabledProperty = booleanSetting(KEY_AUTO_REWIND_ENABLED, DefaultAutoRewindEnabled)
  override var autoRewindEnabled: Boolean by autoRewindEnabledProperty
  override fun observeAutoRewindEnabled(): StateFlow<Boolean> = autoRewindEnabledProperty.observe()

  private val autoRewindAmountProperty = durationSetting(KEY_AUTO_REWIND_AMOUNT, DefaultAutoRewindAmount)
  override var autoRewindAmount: Duration by autoRewindAmountProperty
  override fun observeAutoRewindAmount(): StateFlow<Duration> = autoRewindAmountProperty.observe()
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

private const val DefaultShakeToResetEnabled = false
private const val DefaultAutoSleepTimerEnabled = false
private const val DefaultAutoRewindEnabled = false
private val DefaultAutoRewindAmount = 5.minutes

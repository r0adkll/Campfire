package app.campfire.settings

import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.settings.api.SleepSettings
import app.campfire.settings.api.SleepSettings.AutoSleepTimer
import app.campfire.settings.api.SleepSettings.ShakeSensitivity
import com.r0adkll.kimchi.annotations.ContributesBinding
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalTime
import me.tatarka.inject.annotations.Inject

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = SleepSettings::class)
@Inject
class SleepSettingsImpl(
  override val settings: ObservableSettings,
  private val dispatcherProvider: DispatcherProvider,
) : SleepSettings, AppSettings() {
  private val settingsScope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)
  private val flowSettings by lazy { settings.toFlowSettings(dispatcherProvider.io) }

  override var shakeToResetEnabled: Boolean by booleanSetting(KEY_SHAKE_TO_RESET, DefaultShakeToResetEnabled)
  override fun observeShakeToResetEnabled(): StateFlow<Boolean> {
    return flowSettings.getBooleanFlow(KEY_SHAKE_TO_RESET, DefaultShakeToResetEnabled)
      .stateIn(settingsScope, SharingStarted.Lazily, shakeToResetEnabled)
  }

  override var shakeSensitivity: ShakeSensitivity by enumSetting(KEY_SHAKE_SENSITIVITY, ShakeSensitivity)
  override fun observeShakeSensitivity(): StateFlow<ShakeSensitivity> {
    return flowSettings.getEnumFlow(KEY_SHAKE_SENSITIVITY, ShakeSensitivity)
      .stateIn(settingsScope, SharingStarted.Lazily, shakeSensitivity)
  }

  override var autoSleepTimerEnabled: Boolean by booleanSetting(
    KEY_AUTO_SLEEP_TIMER_ENABLED,
    DefaultAutoSleepTimerEnabled,
  )

  override fun observeAutoSleepTimerEnabled(): StateFlow<Boolean> {
    return flowSettings.getBooleanFlow(KEY_AUTO_SLEEP_TIMER_ENABLED, DefaultAutoSleepTimerEnabled)
      .stateIn(settingsScope, SharingStarted.Lazily, autoSleepTimerEnabled)
  }

  // 10:00 PM
  private val defaultStartTime: LocalTime
    get() = LocalTime(22, 0)

  override var autoSleepStart: LocalTime by localTimeSetting(KEY_AUTO_SLEEP_START, defaultStartTime)
  override fun observeAutoSleepStart(): StateFlow<LocalTime> {
    return flowSettings.getStringOrNullFlow(KEY_AUTO_SLEEP_START)
      .map { value -> value?.let { LocalTime.parse(it) } ?: defaultStartTime }
      .stateIn(settingsScope, SharingStarted.Lazily, autoSleepStart)
  }

  // 6:00 AM
  private val defaultEndTime: LocalTime
    get() = LocalTime(6, 0)

  override var autoSleepEnd: LocalTime by localTimeSetting(KEY_AUTO_SLEEP_END, defaultEndTime)
  override fun observeAutoSleepEnd(): StateFlow<LocalTime> {
    return flowSettings.getStringOrNullFlow(KEY_AUTO_SLEEP_END)
      .map { value -> value?.let { LocalTime.parse(it) } ?: defaultEndTime }
      .stateIn(settingsScope, SharingStarted.Lazily, defaultEndTime)
  }

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

  override var autoSleepTimer: AutoSleepTimer by customSetting(
    key = KEY_AUTO_SLEEP_TIMER,
    defaultValue = AutoSleepTimer.Default,
    getter = timerFromString,
    setter = timerToString,
  )

  override fun observeAutoSleepTimer(): StateFlow<AutoSleepTimer> {
    return flowSettings.getStringOrNullFlow(KEY_AUTO_SLEEP_TIMER)
      .map { value -> value?.let(timerFromString) ?: AutoSleepTimer.Default }
      .stateIn(settingsScope, SharingStarted.Lazily, autoSleepTimer)
  }

  override var autoRewindEnabled: Boolean by booleanSetting(KEY_AUTO_REWIND_ENABLED, DefaultAutoRewindEnabled)
  override fun observeAutoRewindEnabled(): StateFlow<Boolean> {
    return flowSettings.getBooleanFlow(KEY_AUTO_REWIND_ENABLED, DefaultAutoRewindEnabled)
      .stateIn(settingsScope, SharingStarted.Lazily, autoRewindEnabled)
  }

  override var autoRewindAmount: Duration by durationSetting(KEY_AUTO_REWIND_AMOUNT, DefaultAutoRewindAmount)
  override fun observeAutoRewindAmount(): StateFlow<Duration> {
    return flowSettings.getDurationFlow(KEY_AUTO_REWIND_AMOUNT, DefaultAutoRewindAmount)
      .stateIn(settingsScope, SharingStarted.Lazily, autoRewindAmount)
  }
}

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

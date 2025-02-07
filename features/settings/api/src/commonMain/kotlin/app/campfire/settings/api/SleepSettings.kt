package app.campfire.settings.api

import app.campfire.core.settings.EnumSetting
import app.campfire.core.settings.EnumSettingProvider
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalTime

interface SleepSettings {

  var shakeToResetEnabled: Boolean
  fun observeShakeToResetEnabled(): StateFlow<Boolean>

  var shakeSensitivity: ShakeSensitivity
  fun observeShakeSensitivity(): StateFlow<ShakeSensitivity>

  var autoSleepTimerEnabled: Boolean
  fun observeAutoSleepTimerEnabled(): StateFlow<Boolean>

  var autoSleepStart: LocalTime
  fun observeAutoSleepStart(): StateFlow<LocalTime>

  var autoSleepEnd: LocalTime
  fun observeAutoSleepEnd(): StateFlow<LocalTime>

  var autoSleepTimer: AutoSleepTimer
  fun observeAutoSleepTimer(): StateFlow<AutoSleepTimer>

  var autoRewindEnabled: Boolean
  fun observeAutoRewindEnabled(): StateFlow<Boolean>

  var autoRewindAmount: Duration
  fun observeAutoRewindAmount(): StateFlow<Duration>

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
}

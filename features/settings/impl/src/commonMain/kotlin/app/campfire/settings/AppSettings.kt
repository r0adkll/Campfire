package app.campfire.settings

import app.campfire.core.settings.EnumSetting
import app.campfire.core.settings.EnumSettingProvider
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getBooleanFlow
import com.russhwolf.settings.coroutines.getDoubleFlow
import com.russhwolf.settings.coroutines.getFloatFlow
import com.russhwolf.settings.coroutines.getLongFlow
import com.russhwolf.settings.coroutines.getStringFlow
import com.russhwolf.settings.coroutines.getStringOrNullFlow
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

@OptIn(ExperimentalSettingsApi::class)
abstract class AppSettings {
  abstract val scope: CoroutineScope
  abstract val settings: ObservableSettings

  fun booleanSetting(key: String, defaultValue: Boolean = false) = object : SettingsProperty<Boolean> {
    override fun getValue(thisRef: AppSettings, property: KProperty<*>): Boolean {
      return settings.getBoolean(key, defaultValue)
    }

    override fun setValue(thisRef: AppSettings, property: KProperty<*>, value: Boolean) {
      settings.putBoolean(key, value)
    }

    override fun observe(): StateFlow<Boolean> {
      return settings.getBooleanFlow(key, defaultValue)
        .stateIn(scope, SharingStarted.Lazily, settings.getBoolean(key, defaultValue))
    }
  }

  fun longSetting(key: String, defaultValue: Long = 0L) = object : SettingsProperty<Long> {
    override fun getValue(thisRef: AppSettings, property: KProperty<*>): Long {
      return settings.getLong(key, defaultValue)
    }

    override fun setValue(thisRef: AppSettings, property: KProperty<*>, value: Long) {
      settings.putLong(key, value)
    }

    override fun observe(): StateFlow<Long> {
      return settings.getLongFlow(key, defaultValue)
        .stateIn(scope, SharingStarted.Lazily, settings.getLong(key, defaultValue))
    }
  }

  fun floatSetting(key: String, defaultValue: Float = 0f) = object : SettingsProperty<Float> {
    override fun getValue(thisRef: AppSettings, property: KProperty<*>): Float {
      return settings.getFloat(key, defaultValue)
    }

    override fun setValue(thisRef: AppSettings, property: KProperty<*>, value: Float) {
      settings.putFloat(key, value)
    }

    override fun observe(): StateFlow<Float> {
      return settings.getFloatFlow(key, defaultValue)
        .stateIn(scope, SharingStarted.Lazily, settings.getFloat(key, defaultValue))
    }
  }

  fun durationSetting(key: String, defaultValue: Duration) = object : SettingsProperty<Duration> {
    override fun getValue(thisRef: AppSettings, property: KProperty<*>): Duration {
      return settings.getDoubleOrNull(key)?.seconds ?: defaultValue
    }

    override fun setValue(thisRef: AppSettings, property: KProperty<*>, value: Duration) {
      settings.putDouble(key, value.toDouble(DurationUnit.SECONDS))
    }

    override fun observe(): StateFlow<Duration> {
      val currentValue = settings.getDoubleOrNull(key)?.seconds ?: defaultValue
      return settings.getDoubleFlow(key, defaultValue.toDouble(DurationUnit.SECONDS))
        .map { it.seconds }
        .stateIn(scope, SharingStarted.Lazily, currentValue)
    }
  }

  fun stringSetting(key: String, defaultValue: String = "") = object : SettingsProperty<String> {
    override fun getValue(thisRef: AppSettings, property: KProperty<*>): String {
      return settings.getString(key, defaultValue)
    }

    override fun setValue(thisRef: AppSettings, property: KProperty<*>, value: String) {
      settings.putString(key, value)
    }

    override fun observe(): StateFlow<String> {
      return settings.getStringFlow(key, defaultValue)
        .stateIn(scope, SharingStarted.Lazily, settings.getString(key, defaultValue))
    }
  }

  fun stringSetting(key: String, initializer: () -> String) = object : SettingsProperty<String> {
    override fun getValue(thisRef: AppSettings, property: KProperty<*>): String {
      if (!settings.hasKey(key)) {
        settings.putString(key, initializer())
      }
      return settings.getStringOrNull(key)
        ?: throw IllegalStateException("This value should have been initialized")
    }

    override fun setValue(thisRef: AppSettings, property: KProperty<*>, value: String) {
      settings.putString(key, value)
    }

    override fun observe(): StateFlow<String> {
      val defaultValue = if (!settings.hasKey(key)) {
        val value = initializer()
        settings.putString(key, value)
        value
      } else {
        settings.getStringOrNull(key)!!
      }
      return settings.getStringOrNullFlow(key)
        .filterNotNull()
        .stateIn(scope, SharingStarted.Lazily, defaultValue)
    }
  }

  fun stringOrNullSetting(key: String) = object : SettingsProperty<String?> {
    override fun getValue(thisRef: AppSettings, property: KProperty<*>): String? {
      return settings.getStringOrNull(key)
    }

    override fun setValue(thisRef: AppSettings, property: KProperty<*>, value: String?) {
      if (value == null) {
        settings.remove(key)
      } else {
        settings.putString(key, value)
      }
    }

    override fun observe(): StateFlow<String?> {
      return settings.getStringOrNullFlow(key)
        .stateIn(scope, SharingStarted.Lazily, settings.getStringOrNull(key))
    }
  }

  fun localTimeSetting(
    key: String,
    defaultValue: LocalTime,
  ) = object : SettingsProperty<LocalTime> {
    override fun getValue(thisRef: AppSettings, property: KProperty<*>): LocalTime {
      return settings.getStringOrNull(key)
        ?.let { LocalTime.parse(it) }
        ?: defaultValue
    }

    override fun setValue(thisRef: AppSettings, property: KProperty<*>, value: LocalTime) {
      settings.putString(key, value.toString())
    }

    override fun observe(): StateFlow<LocalTime> {
      val currentValue = settings.getStringOrNull(key)?.let { LocalTime.parse(it) } ?: defaultValue
      return settings.getStringOrNullFlow(key)
        .map { raw -> raw?.let { LocalTime.parse(it) } ?: defaultValue }
        .stateIn(scope, SharingStarted.Lazily, currentValue)
    }
  }

  fun localDateTimeSetting(
    key: String,
    defaultValue: LocalDateTime,
  ) = object : SettingsProperty<LocalDateTime> {
    override fun getValue(thisRef: AppSettings, property: KProperty<*>): LocalDateTime {
      return settings.getStringOrNull(key)
        ?.let { LocalDateTime.parse(it) }
        ?: defaultValue
    }

    override fun setValue(thisRef: AppSettings, property: KProperty<*>, value: LocalDateTime) {
      settings.putString(key, value.toString())
    }

    override fun observe(): StateFlow<LocalDateTime> {
      val currentValue = settings.getStringOrNull(key)?.let { LocalDateTime.parse(it) } ?: defaultValue
      return settings.getStringOrNullFlow(key)
        .map { raw -> raw?.let { LocalDateTime.parse(it) } ?: defaultValue }
        .stateIn(scope, SharingStarted.Lazily, currentValue)
    }
  }

  inline fun <reified T> enumSetting(
    key: String,
    provider: EnumSettingProvider<T>,
  ) where T : Enum<T>, T : EnumSetting = object : SettingsProperty<T> {
    override fun getValue(thisRef: AppSettings, property: KProperty<*>): T {
      return settings.getStringOrNull(key).let { storageKey ->
        provider.fromStorageKey(storageKey)
      }
    }

    override fun setValue(thisRef: AppSettings, property: KProperty<*>, value: T) {
      settings.putString(key, value.storageKey)
    }

    override fun observe(): StateFlow<T> {
      val currentValue = settings.getStringOrNull(key).let(provider::fromStorageKey)
      return settings.getStringOrNullFlow(key)
        .map(provider::fromStorageKey)
        .stateIn(scope, SharingStarted.Lazily, currentValue)
    }
  }

  inline fun <reified T> customSetting(
    key: String,
    defaultValue: T,
    crossinline getter: (String) -> T,
    crossinline setter: (T) -> String,
  ) = object : SettingsProperty<T> {
    override fun getValue(thisRef: AppSettings, property: KProperty<*>): T {
      return settings.getStringOrNull(key)?.let(getter) ?: defaultValue
    }

    override fun setValue(thisRef: AppSettings, property: KProperty<*>, value: T) {
      settings.putString(key, setter(value))
    }

    override fun observe(): StateFlow<T> {
      val currentValue = settings.getStringOrNull(key)?.let(getter) ?: defaultValue
      return settings.getStringOrNullFlow(key)
        .map { raw -> raw?.let(getter) ?: defaultValue }
        .stateIn(scope, SharingStarted.Lazily, currentValue)
    }
  }

  interface SettingsProperty<V> : ReadWriteProperty<AppSettings, V> {
    fun observe(): StateFlow<V>
  }
}

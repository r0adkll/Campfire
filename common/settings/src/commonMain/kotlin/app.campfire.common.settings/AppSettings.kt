package app.campfire.common.settings

import app.campfire.core.settings.EnumSetting
import app.campfire.core.settings.EnumSettingProvider
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.FlowSettings
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlinx.coroutines.flow.map

abstract class AppSettings {
  abstract val settings: ObservableSettings

  fun booleanSetting(key: String, defaultValue: Boolean = false) = object : ReadWriteProperty<AppSettings, Boolean> {
    override fun getValue(thisRef: AppSettings, property: KProperty<*>): Boolean {
      return settings.getBoolean(key, defaultValue)
    }

    override fun setValue(thisRef: AppSettings, property: KProperty<*>, value: Boolean) {
      settings.putBoolean(key, value)
    }
  }

  fun longSetting(key: String, defaultValue: Long) = object : ReadWriteProperty<AppSettings, Long> {
    override fun getValue(thisRef: AppSettings, property: KProperty<*>): Long {
      return settings.getLong(key, defaultValue)
    }

    override fun setValue(thisRef: AppSettings, property: KProperty<*>, value: Long) {
      settings.putLong(key, value)
    }
  }

  fun stringSetting(key: String, defaultValue: String = "") = object : ReadWriteProperty<AppSettings, String> {
    override fun getValue(thisRef: AppSettings, property: KProperty<*>): String {
      return settings.getString(key, defaultValue)
    }

    override fun setValue(thisRef: AppSettings, property: KProperty<*>, value: String) {
      settings.putString(key, value)
    }
  }

  fun stringSetting(key: String, initializer: () -> String) = object : ReadWriteProperty<AppSettings, String> {
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
  }

  fun stringOrNullSetting(key: String) = object : ReadWriteProperty<AppSettings, String?> {
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
  }

  inline fun <reified T> enumSetting(
    key: String,
    provider: EnumSettingProvider<T>,
  ) where T : Enum<T>, T : EnumSetting = object : ReadWriteProperty<AppSettings, T> {
    override fun getValue(thisRef: AppSettings, property: KProperty<*>): T {
      return settings.getStringOrNull(key).let { storageKey ->
        provider.fromStorageKey(storageKey)
      }
    }

    override fun setValue(thisRef: AppSettings, property: KProperty<*>, value: T) {
      settings.putString(key, value.storageKey)
    }
  }

  inline fun <reified T> customSetting(
    key: String,
    defaultValue: T,
    crossinline getter: (String) -> T,
    crossinline setter: (T) -> String,
  ) = object : ReadWriteProperty<AppSettings, T> {
    override fun getValue(thisRef: AppSettings, property: KProperty<*>): T {
      return settings.getStringOrNull(key)?.let(getter) ?: defaultValue
    }

    override fun setValue(thisRef: AppSettings, property: KProperty<*>, value: T) {
      settings.putString(key, setter(value))
    }
  }
}

@OptIn(ExperimentalSettingsApi::class)
inline fun <reified T> FlowSettings.getEnumFlow(
  key: String,
  provider: EnumSettingProvider<T>,
) where T : Enum<T>, T : EnumSetting =
  getStringOrNullFlow(key)
    .map(provider::fromStorageKey)

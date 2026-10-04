// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.store

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.campfire.core.settings.EnumSetting
import app.campfire.core.settings.EnumSettingProvider
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

/**
 * Base for the settings implementations: declares typed properties over the [store].
 *
 * Each property is stored under its key with the type the platform preferences held it as before
 * (durations as seconds, everything else that isn't a primitive as a string), so the values
 * [LegacySettingsMigration] copies over read back unchanged.
 */
abstract class AppSettings {
  abstract val store: SettingsStore

  /** Applies [block] to several settings at once, in order with every other write. */
  protected fun edit(block: (MutablePreferences) -> Unit) = store.edit(block)

  fun booleanSetting(key: String, defaultValue: Boolean = false) =
    keySetting(booleanPreferencesKey(key), defaultValue)

  fun longSetting(key: String, defaultValue: Long = 0L) =
    keySetting(longPreferencesKey(key), defaultValue)

  fun floatSetting(key: String, defaultValue: Float = 0f) =
    keySetting(floatPreferencesKey(key), defaultValue)

  fun durationSetting(key: String, defaultValue: Duration): SettingsProperty<Duration> {
    val preferenceKey = doublePreferencesKey(key)
    return setting(
      read = { it[preferenceKey]?.seconds ?: defaultValue },
      write = { preferences, value -> preferences[preferenceKey] = value.toDouble(DurationUnit.SECONDS) },
    )
  }

  fun stringSetting(key: String, defaultValue: String = "") =
    keySetting(stringPreferencesKey(key), defaultValue)

  fun stringOrNullSetting(key: String): SettingsProperty<String?> {
    val preferenceKey = stringPreferencesKey(key)
    return setting(
      read = { it[preferenceKey] },
      write = { preferences, value ->
        if (value == null) preferences.remove(preferenceKey) else preferences[preferenceKey] = value
      },
    )
  }

  /** A string created by [initializer] and stored the first time it's read. */
  fun generatedStringSetting(key: String, initializer: () -> String): GeneratedSetting {
    val preferenceKey = stringPreferencesKey(key)
    return GeneratedSetting {
      store.update { preferences ->
        preferences[preferenceKey] ?: initializer().also { preferences[preferenceKey] = it }
      }
    }
  }

  fun localTimeSetting(key: String, defaultValue: LocalTime) = customSetting(
    key = key,
    defaultValue = defaultValue,
    getter = { LocalTime.parse(it) },
    setter = { it.toString() },
  )

  fun localDateTimeSetting(key: String, defaultValue: LocalDateTime) = customSetting(
    key = key,
    defaultValue = defaultValue,
    getter = { LocalDateTime.parse(it) },
    setter = { it.toString() },
  )

  inline fun <reified T> enumSetting(
    key: String,
    provider: EnumSettingProvider<T>,
  ): SettingsProperty<T> where T : Enum<T>, T : EnumSetting {
    val preferenceKey = stringPreferencesKey(key)
    return setting(
      read = { provider.fromStorageKey(it[preferenceKey]) },
      write = { preferences, value -> preferences[preferenceKey] = value.storageKey },
    )
  }

  inline fun <T> customSetting(
    key: String,
    defaultValue: T,
    crossinline getter: (String) -> T,
    crossinline setter: (T) -> String,
  ): SettingsProperty<T> {
    val preferenceKey = stringPreferencesKey(key)
    return setting(
      read = { preferences -> preferences[preferenceKey]?.let(getter) ?: defaultValue },
      write = { preferences, value -> preferences[preferenceKey] = setter(value) },
    )
  }

  private fun <V : Any> keySetting(key: Preferences.Key<V>, defaultValue: V) = setting(
    read = { it[key] ?: defaultValue },
    write = { preferences, value -> preferences[key] = value },
  )

  /** A setting stored however [read] and [write] say, for values that span keys or need a fallback. */
  fun <V> setting(
    read: (Preferences) -> V,
    write: (MutablePreferences, V) -> Unit,
  ): SettingsProperty<V> = object : SettingsProperty<V> {
    override fun observe(): Flow<V> = store.data.map(read).distinctUntilChanged()

    override suspend fun get(): V = read(store.read())

    override fun set(value: V) = store.edit { write(it, value) }

    override fun update(transform: (V) -> V) = store.edit { write(it, transform(read(it))) }

    override fun readFrom(preferences: Preferences): V = read(preferences)

    override fun writeTo(preferences: MutablePreferences, value: V) = write(preferences, value)
  }

  interface SettingsProperty<V> {
    /** The stored value, then every change to it. */
    fun observe(): Flow<V>

    /** The stored value, after every write made before this call. */
    suspend fun get(): V

    fun set(value: V)

    /** Replaces the stored value with [transform] of it, in order with every other write. */
    fun update(transform: (V) -> V)

    /** This setting's value in [preferences], for reading several settings in one [edit]. */
    fun readFrom(preferences: Preferences): V

    /** Stores [value] in [preferences], for writing several settings in one [edit]. */
    fun writeTo(preferences: MutablePreferences, value: V)
  }

  fun interface GeneratedSetting {
    /** The stored value, created and stored first if there isn't one yet. */
    suspend fun get(): String
  }
}

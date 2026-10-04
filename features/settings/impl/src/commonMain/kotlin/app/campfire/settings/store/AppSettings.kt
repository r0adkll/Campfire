// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.store

import app.campfire.core.settings.EnumSetting
import app.campfire.core.settings.EnumSettingProvider
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SettingsListener
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

/**
 * Base for the settings implementations: declares typed properties over [settings].
 *
 * Every read and write runs on [dispatcher], which runs one task at a time in the order they were
 * submitted. Writes are fire-and-forget, launched in [scope], and a read submitted after a write
 * always sees it.
 */
abstract class AppSettings {
  abstract val scope: CoroutineScope
  abstract val settings: ObservableSettings
  abstract val dispatcher: CoroutineDispatcher

  /** Run [block] after every write submitted before it, without waiting for it. */
  protected fun edit(block: () -> Unit) {
    scope.launch(dispatcher) { block() }
  }

  fun booleanSetting(key: String, defaultValue: Boolean = false) = setting(
    key = key,
    read = { getBoolean(key, defaultValue) },
    write = { putBoolean(key, it) },
    listen = { onChange -> addBooleanOrNullListener(key) { onChange() } },
  )

  fun longSetting(key: String, defaultValue: Long = 0L) = setting(
    key = key,
    read = { getLong(key, defaultValue) },
    write = { putLong(key, it) },
    listen = { onChange -> addLongOrNullListener(key) { onChange() } },
  )

  fun floatSetting(key: String, defaultValue: Float = 0f) = setting(
    key = key,
    read = { getFloat(key, defaultValue) },
    write = { putFloat(key, it) },
    listen = { onChange -> addFloatOrNullListener(key) { onChange() } },
  )

  fun durationSetting(key: String, defaultValue: Duration) = setting(
    key = key,
    read = { getDoubleOrNull(key)?.seconds ?: defaultValue },
    write = { putDouble(key, it.toDouble(DurationUnit.SECONDS)) },
    listen = { onChange -> addDoubleOrNullListener(key) { onChange() } },
  )

  fun stringSetting(key: String, defaultValue: String = "") = setting(
    key = key,
    read = { getString(key, defaultValue) },
    write = { putString(key, it) },
  )

  /** A string created by [initializer] and stored the first time it's read. */
  fun stringSetting(key: String, initializer: () -> String) = setting(
    key = key,
    read = { getStringOrNull(key) ?: initializer().also { putString(key, it) } },
    write = { putString(key, it) },
  )

  fun stringOrNullSetting(key: String) = setting(
    key = key,
    read = { getStringOrNull(key) },
    write = { if (it == null) remove(key) else putString(key, it) },
  )

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
  ) where T : Enum<T>, T : EnumSetting = setting(
    key = key,
    read = { provider.fromStorageKey(getStringOrNull(key)) },
    write = { putString(key, it.storageKey) },
  )

  inline fun <T> customSetting(
    key: String,
    defaultValue: T,
    crossinline getter: (String) -> T,
    crossinline setter: (T) -> String,
  ) = setting(
    key = key,
    read = { getStringOrNull(key)?.let(getter) ?: defaultValue },
    write = { putString(key, setter(it)) },
  )

  fun <V> setting(
    key: String,
    read: ObservableSettings.() -> V,
    write: ObservableSettings.(V) -> Unit,
    listen: ObservableSettings.(onChange: () -> Unit) -> SettingsListener = { onChange ->
      addStringOrNullListener(key) { onChange() }
    },
  ): SettingsProperty<V> = object : SettingsProperty<V> {
    override fun observe(): Flow<V> = callbackFlow {
      send(settings.read())
      val listener = settings.listen { trySend(settings.read()) }
      awaitClose { listener.deactivate() }
    }.flowOn(dispatcher).distinctUntilChanged()

    override suspend fun get(): V = withContext(dispatcher) { settings.read() }

    override fun set(value: V) = edit { settings.write(value) }

    override fun update(transform: (V) -> V) = edit { settings.write(transform(settings.read())) }

    override fun readInEdit(): V = settings.read()

    override fun writeInEdit(value: V) = settings.write(value)
  }

  interface SettingsProperty<V> {
    /** The stored value, then every change to it. */
    fun observe(): Flow<V>

    /** The stored value, after any write submitted before this call. */
    suspend fun get(): V

    fun set(value: V)

    /** Replace the stored value with [transform] of it, ordered with every other write. */
    fun update(transform: (V) -> V)

    /** Reads the stored value directly. Only call it inside [AppSettings.edit]. */
    fun readInEdit(): V

    /** Writes the stored value directly. Only call it inside [AppSettings.edit]. */
    fun writeInEdit(value: V)
  }
}

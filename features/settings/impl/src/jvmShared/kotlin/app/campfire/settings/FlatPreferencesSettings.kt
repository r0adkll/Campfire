// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import com.r0adkll.flatprefs.FlatPreferences
import com.r0adkll.flatprefs.FlatPreferencesStore
import com.r0adkll.flatprefs.PrefKey
import com.r0adkll.flatprefs.booleanKey
import com.r0adkll.flatprefs.doubleKey
import com.r0adkll.flatprefs.floatKey
import com.r0adkll.flatprefs.intKey
import com.r0adkll.flatprefs.longKey
import com.r0adkll.flatprefs.stringKey
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SettingsListener
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * [ObservableSettings] over a [FlatPreferencesStore], so the settings classes keep working unchanged
 * while FlatPrefs stores them.
 *
 * - Reads come from the store's in-memory snapshot. A value stored as another type reads as absent
 *   instead of throwing, as desktop's java.util.prefs did, and is logged once.
 * - Writes go through [FlatPreferencesStore.edit]: visible to reads at once and written in the
 *   background, like `SharedPreferences.apply()`.
 * - Listeners compare each new snapshot with the last one they saw and call back, on
 *   [listenerScope], only when their key's value changed.
 */
internal class FlatPreferencesSettings(
  private val store: FlatPreferencesStore,
  private val listenerScope: CoroutineScope,
) : ObservableSettings {

  private val mismatchedKeys = ConcurrentHashMap.newKeySet<String>()

  override val keys: Set<String> get() = store.current.keys
  override val size: Int get() = store.current.size

  override fun clear() = store.edit { it.clear() }

  // Removing and checking match by name, whatever type the value has
  override fun remove(key: String) = store.edit { it.remove(stringKey(key)) }
  override fun hasKey(key: String): Boolean = stringKey(key) in store

  override fun putInt(key: String, value: Int) = store.edit { it[intKey(key)] = value }
  override fun getInt(key: String, defaultValue: Int): Int = getIntOrNull(key) ?: defaultValue
  override fun getIntOrNull(key: String): Int? = store.current.read(intKey(key))

  override fun putLong(key: String, value: Long) = store.edit { it[longKey(key)] = value }
  override fun getLong(key: String, defaultValue: Long): Long = getLongOrNull(key) ?: defaultValue
  override fun getLongOrNull(key: String): Long? = store.current.read(longKey(key))

  override fun putString(key: String, value: String) = store.edit { it[stringKey(key)] = value }
  override fun getString(key: String, defaultValue: String): String = getStringOrNull(key) ?: defaultValue
  override fun getStringOrNull(key: String): String? = store.current.read(stringKey(key))

  override fun putFloat(key: String, value: Float) = store.edit { it[floatKey(key)] = value }
  override fun getFloat(key: String, defaultValue: Float): Float = getFloatOrNull(key) ?: defaultValue
  override fun getFloatOrNull(key: String): Float? = store.current.read(floatKey(key))

  override fun putDouble(key: String, value: Double) = store.edit { it[doubleKey(key)] = value }
  override fun getDouble(key: String, defaultValue: Double): Double = getDoubleOrNull(key) ?: defaultValue
  override fun getDoubleOrNull(key: String): Double? = store.current.read(doubleKey(key))

  override fun putBoolean(key: String, value: Boolean) = store.edit { it[booleanKey(key)] = value }
  override fun getBoolean(key: String, defaultValue: Boolean): Boolean = getBooleanOrNull(key) ?: defaultValue
  override fun getBooleanOrNull(key: String): Boolean? = store.current.read(booleanKey(key))

  override fun addIntListener(key: String, defaultValue: Int, callback: (Int) -> Unit) =
    addListener(intKey(key), callback) { it ?: defaultValue }

  override fun addLongListener(key: String, defaultValue: Long, callback: (Long) -> Unit) =
    addListener(longKey(key), callback) { it ?: defaultValue }

  override fun addStringListener(key: String, defaultValue: String, callback: (String) -> Unit) =
    addListener(stringKey(key), callback) { it ?: defaultValue }

  override fun addFloatListener(key: String, defaultValue: Float, callback: (Float) -> Unit) =
    addListener(floatKey(key), callback) { it ?: defaultValue }

  override fun addDoubleListener(key: String, defaultValue: Double, callback: (Double) -> Unit) =
    addListener(doubleKey(key), callback) { it ?: defaultValue }

  override fun addBooleanListener(key: String, defaultValue: Boolean, callback: (Boolean) -> Unit) =
    addListener(booleanKey(key), callback) { it ?: defaultValue }

  override fun addIntOrNullListener(key: String, callback: (Int?) -> Unit) =
    addListener(intKey(key), callback) { it }

  override fun addLongOrNullListener(key: String, callback: (Long?) -> Unit) =
    addListener(longKey(key), callback) { it }

  override fun addStringOrNullListener(key: String, callback: (String?) -> Unit) =
    addListener(stringKey(key), callback) { it }

  override fun addFloatOrNullListener(key: String, callback: (Float?) -> Unit) =
    addListener(floatKey(key), callback) { it }

  override fun addDoubleOrNullListener(key: String, callback: (Double?) -> Unit) =
    addListener(doubleKey(key), callback) { it }

  override fun addBooleanOrNullListener(key: String, callback: (Boolean?) -> Unit) =
    addListener(booleanKey(key), callback) { it }

  private inline fun <T : Any, V> addListener(
    key: PrefKey<T>,
    noinline callback: (V) -> Unit,
    crossinline value: (T?) -> V,
  ): SettingsListener {
    var last = value(store.current.read(key))
    // Undispatched, so the listener is subscribed before this returns
    val job = listenerScope.launch(start = CoroutineStart.UNDISPATCHED) {
      store.data.collect { snapshot ->
        val current = value(snapshot.read(key))
        if (current != last) {
          last = current
          callback(current)
        }
      }
    }
    return Listener(job)
  }

  private fun <T : Any> FlatPreferences.read(key: PrefKey<T>): T? = try {
    get(key)
  } catch (e: ClassCastException) {
    if (mismatchedKeys.add(key.name)) {
      bark(LogPriority.WARN, throwable = e) { "Setting '${key.name}' has the wrong type, reading it as unset" }
    }
    null
  }

  private class Listener(private val job: Job) : SettingsListener {
    override fun deactivate() = job.cancel()
  }
}

// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.settings.api.SettingsLoader
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SettingsListener
import kotlin.concurrent.Volatile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * [ObservableSettings] backed by a Preferences [DataStore], so the settings delegates keep their synchronous
 * reads.
 *
 * [startLoading] or [load] reads the file once; after that every read is served from memory. A write updates memory and the
 * key's listeners right away, then persists in the background. This is the only writer of the file, so the
 * background write saves the latest in-memory snapshot: writes that arrive while one is in flight collapse
 * into the next, and they can't land out of order.
 */
class DataStoreSettings(
  private val dataStore: DataStore<Preferences>,
  private val scope: CoroutineScope,
) : ObservableSettings, SettingsLoader {

  @Volatile
  private var snapshot: MutableStateFlow<Preferences>? = null
  private val pendingWrite = Channel<Unit>(Channel.CONFLATED)
  private val listeners = MutableStateFlow<Map<String, List<KeyListener<*>>>>(emptyMap())

  /** Reads the file, running its migrations, then starts persisting writes. */
  private val loading = scope.async(start = CoroutineStart.LAZY) {
    val loaded = MutableStateFlow(dataStore.data.first())
    scope.launch {
      for (signal in pendingWrite) {
        val latest = loaded.value
        try {
          dataStore.updateData { latest }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          // The next write saves the whole snapshot again, so nothing is lost for good
          bark(LogPriority.ERROR, throwable = e) { "Unable to save settings" }
        }
      }
    }
    loaded
  }

  override fun startLoading() {
    loading.start()
  }

  /** Must finish before any other call. */
  override suspend fun load() {
    snapshot = loading.await()
  }

  private val preferences: Preferences
    get() = checkNotNull(snapshot) { "Settings were read before they were loaded" }.value

  private fun edit(changedKeys: Collection<String>, block: (MutablePreferences) -> Unit) {
    val state = checkNotNull(snapshot) { "Settings were written before they were loaded" }
    state.update { it.toMutablePreferences().apply(block).toPreferences() }
    pendingWrite.trySend(Unit)
    val current = listeners.value
    changedKeys.forEach { key -> current[key]?.forEach { it.onChanged() } }
  }

  private fun <T : Any> put(key: Preferences.Key<T>, value: T) = edit(listOf(key.name)) {
    // Keys are equal by name alone, so this also replaces a value of another type stored under the same name
    it.remove(key)
    it[key] = value
  }

  // Any typed key finds a value by name; the cast then checks the type the caller asked for
  private inline fun <reified T> Preferences.valueOf(key: String): T? = asMap()[stringPreferencesKey(key)] as? T

  override val keys: Set<String> get() = preferences.asMap().keys.mapTo(mutableSetOf()) { it.name }
  override val size: Int get() = preferences.asMap().size

  override fun clear() {
    val cleared = keys
    edit(cleared) { it.clear() }
  }

  override fun remove(key: String) = edit(listOf(key)) { it.remove(stringPreferencesKey(key)) }

  override fun hasKey(key: String): Boolean = stringPreferencesKey(key) in preferences.asMap()

  override fun putInt(key: String, value: Int) = put(intPreferencesKey(key), value)
  override fun getInt(key: String, defaultValue: Int): Int = getIntOrNull(key) ?: defaultValue
  override fun getIntOrNull(key: String): Int? = preferences.valueOf(key)

  override fun putLong(key: String, value: Long) = put(longPreferencesKey(key), value)
  override fun getLong(key: String, defaultValue: Long): Long = getLongOrNull(key) ?: defaultValue
  override fun getLongOrNull(key: String): Long? = preferences.valueOf(key)

  override fun putString(key: String, value: String) = put(stringPreferencesKey(key), value)
  override fun getString(key: String, defaultValue: String): String = getStringOrNull(key) ?: defaultValue
  override fun getStringOrNull(key: String): String? = preferences.valueOf(key)

  override fun putFloat(key: String, value: Float) = put(floatPreferencesKey(key), value)
  override fun getFloat(key: String, defaultValue: Float): Float = getFloatOrNull(key) ?: defaultValue
  override fun getFloatOrNull(key: String): Float? = preferences.valueOf(key)

  override fun putDouble(key: String, value: Double) = put(doublePreferencesKey(key), value)
  override fun getDouble(key: String, defaultValue: Double): Double = getDoubleOrNull(key) ?: defaultValue
  override fun getDoubleOrNull(key: String): Double? = preferences.valueOf(key)

  override fun putBoolean(key: String, value: Boolean) = put(booleanPreferencesKey(key), value)
  override fun getBoolean(key: String, defaultValue: Boolean): Boolean = getBooleanOrNull(key) ?: defaultValue
  override fun getBooleanOrNull(key: String): Boolean? = preferences.valueOf(key)

  override fun addIntListener(key: String, defaultValue: Int, callback: (Int) -> Unit) =
    addListener(key, { getInt(key, defaultValue) }, callback)
  override fun addLongListener(key: String, defaultValue: Long, callback: (Long) -> Unit) =
    addListener(key, { getLong(key, defaultValue) }, callback)
  override fun addStringListener(key: String, defaultValue: String, callback: (String) -> Unit) =
    addListener(key, { getString(key, defaultValue) }, callback)
  override fun addFloatListener(key: String, defaultValue: Float, callback: (Float) -> Unit) =
    addListener(key, { getFloat(key, defaultValue) }, callback)
  override fun addDoubleListener(key: String, defaultValue: Double, callback: (Double) -> Unit) =
    addListener(key, { getDouble(key, defaultValue) }, callback)
  override fun addBooleanListener(key: String, defaultValue: Boolean, callback: (Boolean) -> Unit) =
    addListener(key, { getBoolean(key, defaultValue) }, callback)

  override fun addIntOrNullListener(key: String, callback: (Int?) -> Unit) =
    addListener(key, { getIntOrNull(key) }, callback)
  override fun addLongOrNullListener(key: String, callback: (Long?) -> Unit) =
    addListener(key, { getLongOrNull(key) }, callback)
  override fun addStringOrNullListener(key: String, callback: (String?) -> Unit) =
    addListener(key, { getStringOrNull(key) }, callback)
  override fun addFloatOrNullListener(key: String, callback: (Float?) -> Unit) =
    addListener(key, { getFloatOrNull(key) }, callback)
  override fun addDoubleOrNullListener(key: String, callback: (Double?) -> Unit) =
    addListener(key, { getDoubleOrNull(key) }, callback)
  override fun addBooleanOrNullListener(key: String, callback: (Boolean?) -> Unit) =
    addListener(key, { getBooleanOrNull(key) }, callback)

  private fun <T> addListener(key: String, read: () -> T, callback: (T) -> Unit): SettingsListener {
    val listener = KeyListener(read, callback)
    listeners.update { it + (key to it[key].orEmpty() + listener) }
    return object : SettingsListener {
      override fun deactivate() {
        listeners.update { current -> current + (key to current[key].orEmpty() - listener) }
      }
    }
  }

  /** Calls back only when the value read for its key actually changes, as the platform listeners do. */
  private class KeyListener<T>(
    private val read: () -> T,
    private val callback: (T) -> Unit,
  ) {
    private var last: T = read()

    fun onChanged() {
      val value = read()
      if (value == last) return
      last = value
      callback(value)
    }
  }
}

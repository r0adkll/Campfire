// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.securesettings

import com.russhwolf.settings.Settings
import kotlin.io.encoding.Base64

/**
 * A [Settings] that stores every value encrypted by [cipher] in [delegate], as Base64 text. Keys
 * are stored in the clear. A value that no longer decrypts reads as absent.
 *
 * [delegate] is resolved on first use so opening the backing store (and any one-time migration
 * into it) happens on the caller's thread, not wherever the instance is constructed.
 */
class EncryptedSettings(
  private val cipher: SettingsCipher,
  delegate: () -> Settings,
) : Settings {

  private val delegate by lazy(delegate)

  override val keys: Set<String> get() = delegate.keys
  override val size: Int get() = delegate.size

  override fun clear() = delegate.clear()
  override fun remove(key: String) = delegate.remove(key)
  override fun hasKey(key: String): Boolean = delegate.hasKey(key)

  override fun putString(key: String, value: String) {
    val ciphertext = cipher.encrypt(value.encodeToByteArray(), key.encodeToByteArray())
    delegate.putString(key, Base64.encode(ciphertext))
  }

  override fun getStringOrNull(key: String): String? {
    val stored = delegate.getStringOrNull(key) ?: return null
    val ciphertext = runCatching { Base64.decode(stored) }.getOrNull() ?: return null
    return cipher.decrypt(ciphertext, key.encodeToByteArray())?.decodeToString()
  }

  override fun getString(key: String, defaultValue: String): String = getStringOrNull(key) ?: defaultValue

  override fun putInt(key: String, value: Int) = putString(key, value.toString())
  override fun getIntOrNull(key: String): Int? = getStringOrNull(key)?.toIntOrNull()
  override fun getInt(key: String, defaultValue: Int): Int = getIntOrNull(key) ?: defaultValue

  override fun putLong(key: String, value: Long) = putString(key, value.toString())
  override fun getLongOrNull(key: String): Long? = getStringOrNull(key)?.toLongOrNull()
  override fun getLong(key: String, defaultValue: Long): Long = getLongOrNull(key) ?: defaultValue

  override fun putFloat(key: String, value: Float) = putString(key, value.toString())
  override fun getFloatOrNull(key: String): Float? = getStringOrNull(key)?.toFloatOrNull()
  override fun getFloat(key: String, defaultValue: Float): Float = getFloatOrNull(key) ?: defaultValue

  override fun putDouble(key: String, value: Double) = putString(key, value.toString())
  override fun getDoubleOrNull(key: String): Double? = getStringOrNull(key)?.toDoubleOrNull()
  override fun getDouble(key: String, defaultValue: Double): Double = getDoubleOrNull(key) ?: defaultValue

  override fun putBoolean(key: String, value: Boolean) = putString(key, value.toString())
  override fun getBooleanOrNull(key: String): Boolean? = getStringOrNull(key)?.toBooleanStrictOrNull()
  override fun getBoolean(key: String, defaultValue: Boolean): Boolean = getBooleanOrNull(key) ?: defaultValue
}

/**
 * Copies [entries] (as read from a legacy store) into this one, keeping their types. Entries of a
 * type [Settings] can't hold are skipped.
 */
fun Settings.importEntries(entries: Map<String, *>) {
  entries.forEach { (key, value) ->
    when (value) {
      is String -> putString(key, value)
      is Boolean -> putBoolean(key, value)
      is Int -> putInt(key, value)
      is Long -> putLong(key, value)
      is Float -> putFloat(key, value)
      is Double -> putDouble(key, value)
      else -> Unit
    }
  }
}

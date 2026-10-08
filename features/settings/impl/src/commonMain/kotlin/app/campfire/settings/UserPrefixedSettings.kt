// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.model.UserId
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SettingsListener

/**
 * One account's settings, kept in the app's [ObservableSettings] under keys prefixed with the account's id. They
 * share the app's storage, and its backup, without ever mixing with another account's. [keys], [size] and [clear]
 * only see this account's settings.
 */
internal class UserPrefixedSettings(
  private val delegate: ObservableSettings,
  userId: UserId,
) : ObservableSettings {

  // Short, since desktop's java.util.prefs rejects keys longer than 80 characters
  private val prefix = "user:$userId:"

  private fun key(key: String) = prefix + key

  override val keys: Set<String>
    get() = delegate.keys.filter { it.startsWith(prefix) }.map { it.removePrefix(prefix) }.toSet()

  override val size: Int get() = keys.size

  override fun clear() = delegate.keys.filter { it.startsWith(prefix) }.forEach(delegate::remove)
  override fun remove(key: String) = delegate.remove(key(key))
  override fun hasKey(key: String): Boolean = delegate.hasKey(key(key))

  override fun putInt(key: String, value: Int) = delegate.putInt(key(key), value)
  override fun getInt(key: String, defaultValue: Int): Int = delegate.getInt(key(key), defaultValue)
  override fun getIntOrNull(key: String): Int? = delegate.getIntOrNull(key(key))

  override fun putLong(key: String, value: Long) = delegate.putLong(key(key), value)
  override fun getLong(key: String, defaultValue: Long): Long = delegate.getLong(key(key), defaultValue)
  override fun getLongOrNull(key: String): Long? = delegate.getLongOrNull(key(key))

  override fun putString(key: String, value: String) = delegate.putString(key(key), value)
  override fun getString(key: String, defaultValue: String): String = delegate.getString(key(key), defaultValue)
  override fun getStringOrNull(key: String): String? = delegate.getStringOrNull(key(key))

  override fun putFloat(key: String, value: Float) = delegate.putFloat(key(key), value)
  override fun getFloat(key: String, defaultValue: Float): Float = delegate.getFloat(key(key), defaultValue)
  override fun getFloatOrNull(key: String): Float? = delegate.getFloatOrNull(key(key))

  override fun putDouble(key: String, value: Double) = delegate.putDouble(key(key), value)
  override fun getDouble(key: String, defaultValue: Double): Double = delegate.getDouble(key(key), defaultValue)
  override fun getDoubleOrNull(key: String): Double? = delegate.getDoubleOrNull(key(key))

  override fun putBoolean(key: String, value: Boolean) = delegate.putBoolean(key(key), value)
  override fun getBoolean(key: String, defaultValue: Boolean): Boolean = delegate.getBoolean(key(key), defaultValue)
  override fun getBooleanOrNull(key: String): Boolean? = delegate.getBooleanOrNull(key(key))

  override fun addIntListener(key: String, defaultValue: Int, callback: (Int) -> Unit): SettingsListener =
    delegate.addIntListener(key(key), defaultValue, callback)

  override fun addLongListener(key: String, defaultValue: Long, callback: (Long) -> Unit): SettingsListener =
    delegate.addLongListener(key(key), defaultValue, callback)

  override fun addStringListener(key: String, defaultValue: String, callback: (String) -> Unit): SettingsListener =
    delegate.addStringListener(key(key), defaultValue, callback)

  override fun addFloatListener(key: String, defaultValue: Float, callback: (Float) -> Unit): SettingsListener =
    delegate.addFloatListener(key(key), defaultValue, callback)

  override fun addDoubleListener(key: String, defaultValue: Double, callback: (Double) -> Unit): SettingsListener =
    delegate.addDoubleListener(key(key), defaultValue, callback)

  override fun addBooleanListener(key: String, defaultValue: Boolean, callback: (Boolean) -> Unit): SettingsListener =
    delegate.addBooleanListener(key(key), defaultValue, callback)

  override fun addIntOrNullListener(key: String, callback: (Int?) -> Unit): SettingsListener =
    delegate.addIntOrNullListener(key(key), callback)

  override fun addLongOrNullListener(key: String, callback: (Long?) -> Unit): SettingsListener =
    delegate.addLongOrNullListener(key(key), callback)

  override fun addStringOrNullListener(key: String, callback: (String?) -> Unit): SettingsListener =
    delegate.addStringOrNullListener(key(key), callback)

  override fun addFloatOrNullListener(key: String, callback: (Float?) -> Unit): SettingsListener =
    delegate.addFloatOrNullListener(key(key), callback)

  override fun addDoubleOrNullListener(key: String, callback: (Double?) -> Unit): SettingsListener =
    delegate.addDoubleOrNullListener(key(key), callback)

  override fun addBooleanOrNullListener(key: String, callback: (Boolean?) -> Unit): SettingsListener =
    delegate.addBooleanOrNullListener(key(key), callback)
}

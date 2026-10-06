// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import com.r0adkll.flatprefs.FlatPreferences
import com.r0adkll.flatprefs.FlatPreferencesMigration
import com.r0adkll.flatprefs.MutableFlatPreferences
import com.r0adkll.flatprefs.booleanKey
import com.r0adkll.flatprefs.doubleKey
import com.r0adkll.flatprefs.floatKey
import com.r0adkll.flatprefs.intKey
import com.r0adkll.flatprefs.longKey
import com.r0adkll.flatprefs.stringKey
import java.util.prefs.BackingStoreException
import java.util.prefs.Preferences

/**
 * Moves the settings out of desktop's java.util.prefs node into FlatPrefs, keeping their types.
 *
 * The node keeps every value as text, so each key's type comes from [LegacySettingTypes]. Keys it
 * doesn't list stay in the node: the account tokens, extra headers and Hardcover credentials share
 * it and are still read from there. A value that doesn't parse as its type is dropped, as
 * java.util.prefs already read it as the default.
 *
 * A key the store already has keeps the store's value: an earlier run migrated it, and its removal
 * from the node never reached disk, so the node's copy is older than any change made since.
 */
internal class JavaPreferencesMigration(
  private val preferences: Preferences,
) : FlatPreferencesMigration {

  // What migrate() handled, so cleanUp() removes exactly that
  private var migratedKeys: Set<String> = emptySet()

  override fun shouldMigrate(current: FlatPreferences): Boolean =
    preferences.keys().any { LegacySettingTypes.typeOf(it) != null }

  override fun migrate(prefs: MutableFlatPreferences) {
    val migrated = mutableSetOf<String>()
    for (key in preferences.keys()) {
      val type = LegacySettingTypes.typeOf(key) ?: continue
      migrated += key
      if (prefs.valueOf(key) != null) continue
      val text = preferences.get(key, null) ?: continue
      if (!prefs.putParsed(key, type, text)) {
        bark(LogPriority.WARN) { "Setting '$key' isn't a valid $type, dropping it" }
      }
    }
    migratedKeys = migrated
  }

  override fun cleanUp() {
    migratedKeys.forEach(preferences::remove)
    try {
      preferences.flush()
    } catch (e: BackingStoreException) {
      // The removals are still pending in memory, and java.util.prefs retries them on its next sync
      bark(LogPriority.WARN, throwable = e) { "Unable to remove the migrated settings from the old preferences" }
    }
  }

  /** Parses [text] the way java.util.prefs' typed getters did, storing it if it's valid. */
  private fun MutableFlatPreferences.putParsed(key: String, type: LegacySettingType, text: String): Boolean {
    when (type) {
      LegacySettingType.Boolean -> when {
        text.equals("true", ignoreCase = true) -> set(booleanKey(key), true)
        text.equals("false", ignoreCase = true) -> set(booleanKey(key), false)
        else -> return false
      }
      LegacySettingType.Int -> set(intKey(key), text.toIntOrNull() ?: return false)
      LegacySettingType.Long -> set(longKey(key), text.toLongOrNull() ?: return false)
      LegacySettingType.Float -> set(floatKey(key), text.toFloatOrNull() ?: return false)
      LegacySettingType.Double -> set(doubleKey(key), text.toDoubleOrNull() ?: return false)
      LegacySettingType.String -> set(stringKey(key), text)
    }
    return true
  }
}

// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import com.r0adkll.flatprefs.FlatPreferences
import com.r0adkll.flatprefs.FlatPreferencesCorruptionException
import com.r0adkll.flatprefs.FlatPreferencesMigration
import com.r0adkll.flatprefs.FlatPreferencesStore
import com.r0adkll.flatprefs.MutableFlatPreferences
import com.r0adkll.flatprefs.open
import okio.Path

/** The settings store's name: `files/flatprefs/settings.fpb` on Android, `flatprefs/settings.fpb` on desktop. */
internal const val SETTINGS_STORE_NAME = "settings"

private const val TAG = "SettingsStore"

/** A damaged file starts over, as a damaged SharedPreferences file does. */
internal fun replaceDamagedSettings(e: FlatPreferencesCorruptionException): FlatPreferences {
  bark(TAG, LogPriority.ERROR, throwable = e) { "Settings file is damaged, starting over" }
  return FlatPreferences.EMPTY
}

internal fun logSettingsWriteError(e: Throwable) {
  bark(TAG, LogPriority.ERROR, throwable = e) { "Unable to save settings" }
}

/** The migrated settings are still served; the old ones are kept and migrated again next launch. */
internal fun logSettingsMigrationError(e: Throwable) {
  bark(TAG, LogPriority.ERROR, throwable = e) { "Unable to finish migrating settings" }
}

/** Opens the settings store kept in [file], running [migrations] on its first load. */
internal fun openSettingsStore(
  file: Path,
  migrations: List<FlatPreferencesMigration>,
): FlatPreferencesStore = FlatPreferencesStore.open(
  path = file,
  onCorruption = ::replaceDamagedSettings,
  onWriteError = ::logSettingsWriteError,
  migrations = migrations,
  onMigrationError = ::logSettingsMigrationError,
)

/**
 * multiplatform-settings kept each double in SharedPreferences as a long holding its bits, which
 * `SharedPreferencesMigration` would copy as a long. Turns the settings read as doubles back into
 * doubles.
 */
internal fun restoreLegacyDouble(key: String, value: Any): Any =
  if (value is Long && key in LegacySettingTypes.doubleKeys) Double.fromBits(value) else value

internal fun MutableFlatPreferences.clearDeviceBoundSettings() {
  DeviceBoundSettingKeys.forEach(::remove)
}

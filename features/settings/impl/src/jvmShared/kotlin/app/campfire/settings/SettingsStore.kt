// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import com.r0adkll.flatprefs.FileStorage
import com.r0adkll.flatprefs.FlatPreferences
import com.r0adkll.flatprefs.FlatPreferencesMigration
import com.r0adkll.flatprefs.FlatPreferencesStore
import com.r0adkll.flatprefs.MutableFlatPreferences
import com.r0adkll.flatprefs.stringKey
import okio.Path

/** The settings file's name inside its `flatprefs` directory, as `FlatPreferencesStore.open(context, name)` names it. */
internal const val SETTINGS_STORE_FILE_NAME = "settings.fpb"

private const val TAG = "SettingsStore"

/**
 * Opens the settings store kept in [file], running [migrations] on its first load. The Android backup
 * agent opens it here too, without the DI graph.
 */
internal fun openSettingsStore(
  file: Path,
  migrations: List<FlatPreferencesMigration>,
): FlatPreferencesStore = FlatPreferencesStore.open(
  storage = FileStorage(file),
  // A damaged file starts over, as a damaged SharedPreferences file does
  onCorruption = { e ->
    bark(TAG, LogPriority.ERROR, throwable = e) { "Settings file is damaged, starting over" }
    FlatPreferences.EMPTY
  },
  onWriteError = { e ->
    bark(TAG, LogPriority.ERROR, throwable = e) { "Unable to save settings" }
  },
  migrations = migrations,
)

internal fun MutableFlatPreferences.clearDeviceBoundSettings() {
  // Keys match by name, whatever type the value was stored as
  DeviceBoundSettingKeys.forEach { remove(stringKey(it)) }
}

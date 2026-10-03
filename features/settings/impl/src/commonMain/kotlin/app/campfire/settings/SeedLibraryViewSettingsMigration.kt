// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.russhwolf.settings.Settings

/**
 * Starts an account's settings from the library view settings the app kept for everyone before they moved to
 * each account, so nobody's sorting or layout resets. Accounts added later start from the same values.
 */
class SeedLibraryViewSettingsMigration(
  private val appSettings: Settings,
) : DataMigration<Preferences> {

  override suspend fun shouldMigrate(currentData: Preferences): Boolean = currentData[SEEDED] != true

  override suspend fun migrate(currentData: Preferences): Preferences {
    val seeded = currentData.toMutablePreferences()
    LibraryViewStringKeys.forEach { key ->
      appSettings.getStringOrNull(key)?.let { seeded[stringPreferencesKey(key)] = it }
    }
    LibraryViewBooleanKeys.forEach { key ->
      appSettings.getBooleanOrNull(key)?.let { seeded[booleanPreferencesKey(key)] = it }
    }
    seeded[SEEDED] = true
    return seeded.toPreferences()
  }

  override suspend fun cleanUp() = Unit

  companion object {
    private val SEEDED = booleanPreferencesKey("library_view_settings_seeded")
  }
}

internal val LibraryViewStringKeys = listOf(
  KEY_LIBRARY_ITEM_DISPLAY_STATE,
  KEY_SORT_MODE,
  KEY_SORT_DIRECTION,
  KEY_AUTHOR_SORT_MODE,
  KEY_AUTHORS_SORT_DIRECTION,
  KEY_SERIES_SORT_MODE,
  KEY_SERIES_SORT_DIRECTION,
  KEY_SERIES_DISPLAY_STATE,
  KEY_COLLECTIONS_DISPLAY_STATE,
  KEY_PLAYLISTS_DISPLAY_STATE,
)

internal val LibraryViewBooleanKeys = listOf(
  KEY_LIBRARY_ITEM_MARQUEE,
  KEY_SHOW_CONFIRM_DOWNLOAD,
  KEY_SHOW_TIME_IN_BOOK,
)

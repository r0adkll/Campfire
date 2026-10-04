// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.user

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import app.campfire.settings.library.KEY_AUTHORS_SORT_DIRECTION
import app.campfire.settings.library.KEY_AUTHOR_SORT_MODE
import app.campfire.settings.library.KEY_COLLECTIONS_DISPLAY_STATE
import app.campfire.settings.library.KEY_LIBRARY_ITEM_DISPLAY_STATE
import app.campfire.settings.library.KEY_LIBRARY_ITEM_MARQUEE
import app.campfire.settings.library.KEY_PLAYLISTS_DISPLAY_STATE
import app.campfire.settings.library.KEY_SERIES_DISPLAY_STATE
import app.campfire.settings.library.KEY_SERIES_SORT_DIRECTION
import app.campfire.settings.library.KEY_SERIES_SORT_MODE
import app.campfire.settings.library.KEY_SHOW_CONFIRM_DOWNLOAD
import app.campfire.settings.library.KEY_SHOW_TIME_IN_BOOK
import app.campfire.settings.library.KEY_SORT_DIRECTION
import app.campfire.settings.library.KEY_SORT_MODE
import kotlinx.coroutines.flow.first

/**
 * Starts an account's settings from the values the app kept for everyone before they moved to each account,
 * so nobody's theme, library layout or per-book choices reset. Accounts added later start from the same
 * values. The app-wide values are left in place for a release, so a downgrade still finds them.
 */
class SeedUserSettingsMigration(
  private val appDataStore: DataStore<Preferences>,
) : DataMigration<Preferences> {

  override suspend fun shouldMigrate(currentData: Preferences): Boolean = currentData[SEEDED] != true

  override suspend fun migrate(currentData: Preferences): Preferences {
    val seeded = currentData.toMutablePreferences()
    appDataStore.data.first().asMap().forEach { (key, value) ->
      if (key.name in UserSettingKeys && key !in currentData) {
        // Copied with the type the app stored it as
        @Suppress("UNCHECKED_CAST")
        seeded[key as Preferences.Key<Any>] = value
      }
    }
    seeded[SEEDED] = true
    return seeded.toPreferences()
  }

  override suspend fun cleanUp() = Unit

  companion object {
    private val SEEDED = booleanPreferencesKey("user_settings_seeded")
  }
}

/** Every setting each account keeps for itself. */
internal val UserSettingKeys = setOf(
  KEY_CURRENT_THEME,
  KEY_LIBRARY_ITEM_DISPLAY_STATE,
  KEY_LIBRARY_ITEM_MARQUEE,
  KEY_SORT_MODE,
  KEY_SORT_DIRECTION,
  KEY_AUTHOR_SORT_MODE,
  KEY_AUTHORS_SORT_DIRECTION,
  KEY_SERIES_SORT_MODE,
  KEY_SERIES_SORT_DIRECTION,
  KEY_SERIES_DISPLAY_STATE,
  KEY_COLLECTIONS_DISPLAY_STATE,
  KEY_PLAYLISTS_DISPLAY_STATE,
  KEY_SHOW_CONFIRM_DOWNLOAD,
  KEY_SHOW_TIME_IN_BOOK,
  PREF_ITEM_PLAYBACK_SPEEDS,
  PREF_ITEM_EQUALIZER_PROFILES,
)

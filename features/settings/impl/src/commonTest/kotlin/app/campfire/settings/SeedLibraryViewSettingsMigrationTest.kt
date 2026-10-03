// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class SeedLibraryViewSettingsMigrationTest {

  private val appSettings = MapSettings()
  private val migration = SeedLibraryViewSettingsMigration(appSettings)

  @Test
  fun `copies the library view settings`() = runTest {
    appSettings.putString(KEY_SERIES_DISPLAY_STATE, "list")
    appSettings.putBoolean(KEY_LIBRARY_ITEM_MARQUEE, false)

    val seeded = migration.migrate(emptyPreferences())

    assertThat(seeded[stringPreferencesKey(KEY_SERIES_DISPLAY_STATE)]).isEqualTo("list")
    assertThat(seeded[booleanPreferencesKey(KEY_LIBRARY_ITEM_MARQUEE)]).isEqualTo(false)
  }

  @Test
  fun `leaves out other settings and unset values`() = runTest {
    appSettings.putString(KEY_THEME, "dark")

    val seeded = migration.migrate(emptyPreferences())

    assertThat(seeded[stringPreferencesKey(KEY_THEME)]).isNull()
    assertThat(seeded[stringPreferencesKey(KEY_SORT_MODE)]).isNull()
  }

  @Test
  fun `seeds once`() = runTest {
    assertThat(migration.shouldMigrate(emptyPreferences())).isTrue()
    assertThat(migration.shouldMigrate(migration.migrate(emptyPreferences()))).isFalse()
  }
}

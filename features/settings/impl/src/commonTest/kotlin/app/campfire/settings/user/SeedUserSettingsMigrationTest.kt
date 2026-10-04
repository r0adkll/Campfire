// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.user

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import app.campfire.settings.library.KEY_SHOW_TIME_IN_BOOK
import app.campfire.settings.library.KEY_SORT_MODE
import app.campfire.settings.playback.PREF_PLAYBACK_SPEED
import app.campfire.settings.store.InMemoryPreferencesDataStore
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class SeedUserSettingsMigrationTest {

  private val app = InMemoryPreferencesDataStore(
    mutablePreferencesOf(
      stringPreferencesKey(KEY_SORT_MODE) to "duration",
      booleanPreferencesKey(KEY_SHOW_TIME_IN_BOOK) to false,
      stringPreferencesKey(KEY_CURRENT_THEME) to "forest",
      stringPreferencesKey(PREF_ITEM_PLAYBACK_SPEEDS) to "li_abc123|1.5",
      floatPreferencesKey(PREF_PLAYBACK_SPEED) to 1.25f,
    ),
  )
  private val migration = SeedUserSettingsMigration(app)

  @Test
  fun `copies the account's settings with their types`() = runTest {
    val seeded = migration.migrate(emptyPreferences())

    assertThat(seeded[stringPreferencesKey(KEY_SORT_MODE)]).isEqualTo("duration")
    assertThat(seeded[booleanPreferencesKey(KEY_SHOW_TIME_IN_BOOK)]).isEqualTo(false)
    assertThat(seeded[stringPreferencesKey(KEY_CURRENT_THEME)]).isEqualTo("forest")
    assertThat(seeded[stringPreferencesKey(PREF_ITEM_PLAYBACK_SPEEDS)]).isEqualTo("li_abc123|1.5")
  }

  @Test
  fun `leaves the app-wide settings out`() = runTest {
    val seeded = migration.migrate(emptyPreferences())

    assertThat(seeded[floatPreferencesKey(PREF_PLAYBACK_SPEED)]).isNull()
  }

  @Test
  fun `keeps what the account already has`() = runTest {
    val existing = mutablePreferencesOf(stringPreferencesKey(KEY_SORT_MODE) to "title")

    val seeded = migration.migrate(existing)

    assertThat(seeded[stringPreferencesKey(KEY_SORT_MODE)]).isEqualTo("title")
  }

  @Test
  fun `runs once`() = runTest {
    assertThat(migration.shouldMigrate(emptyPreferences())).isTrue()
    val seeded = migration.migrate(emptyPreferences())
    assertThat(migration.shouldMigrate(seeded)).isFalse()
  }
}

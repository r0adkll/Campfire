// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class LegacySettingsMigrationTest {

  private val legacy = MapSettings()
  private val migration = LegacySettingsMigration(legacy)

  @Test
  fun `copies each kind of value with its type`() = runTest {
    legacy.putBoolean(KEY_HAS_CONSENTED, true)
    legacy.putLong(PREF_FORWARD_TIME_MS, 45_000L)
    legacy.putFloat(PREF_PLAYBACK_SPEED, 1.5f)
    legacy.putDouble(KEY_FADE_OUT_DURATION, 12.0)
    legacy.putString(KEY_THEME, "dark")

    val migrated = migration.migrate(emptyPreferences())

    assertThat(migrated[booleanPreferencesKey(KEY_HAS_CONSENTED)]).isEqualTo(true)
    assertThat(migrated[longPreferencesKey(PREF_FORWARD_TIME_MS)]).isEqualTo(45_000L)
    assertThat(migrated[floatPreferencesKey(PREF_PLAYBACK_SPEED)]).isEqualTo(1.5f)
    assertThat(migrated[doublePreferencesKey(KEY_FADE_OUT_DURATION)]).isEqualTo(12.0)
    assertThat(migrated[stringPreferencesKey(KEY_THEME)]).isEqualTo("dark")
  }

  @Test
  fun `leaves out what was never set`() = runTest {
    val migrated = migration.migrate(emptyPreferences())

    assertThat(migrated[stringPreferencesKey(KEY_THEME)]).isNull()
  }

  @Test
  fun `runs once`() = runTest {
    assertThat(migration.shouldMigrate(emptyPreferences())).isTrue()
    val migrated = migration.migrate(emptyPreferences())
    assertThat(migration.shouldMigrate(migrated)).isFalse()
  }

  @Test
  fun `keeps values already in DataStore`() = runTest {
    legacy.putString(KEY_THEME, "dark")
    val existing = emptyPreferences().toMutablePreferences().apply {
      this[stringPreferencesKey(KEY_CURRENT_THEME)] = "campfire"
    }

    val migrated = migration.migrate(existing)

    assertThat(migrated[stringPreferencesKey(KEY_CURRENT_THEME)]).isEqualTo("campfire")
    assertThat(migrated[stringPreferencesKey(KEY_THEME)]).isEqualTo("dark")
  }

  @Test
  fun `leaves the old values in place`() = runTest {
    legacy.putString(KEY_THEME, "dark")
    migration.migrate(emptyPreferences())
    assertThat(legacy.getStringOrNull(KEY_THEME)).isEqualTo("dark")
  }

  @Test
  fun `lists every key once`() {
    val keys = LegacyBooleanKeys + LegacyLongKeys + LegacyFloatKeys + LegacyDoubleKeys + LegacyStringKeys
    assertThat(keys.toSet().size).isEqualTo(keys.size)
  }
}

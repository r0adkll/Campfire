// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.store

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.campfire.settings.api.ThemeMode
import app.campfire.settings.app.KEY_HAS_CONSENTED
import app.campfire.settings.app.PrivacySettingsImpl
import app.campfire.settings.playback.KEY_FADE_OUT_DURATION
import app.campfire.settings.playback.PREF_FORWARD_TIME_MS
import app.campfire.settings.playback.PREF_PLAYBACK_SPEED
import app.campfire.settings.playback.PlaybackSettingsImpl
import app.campfire.settings.playback.SleepSettingsImpl
import app.campfire.settings.theme.KEY_THEME
import app.campfire.settings.theme.ThemeSettingsImpl
import app.campfire.settings.user.KEY_CURRENT_THEME
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
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
  fun `migrated values read back through the settings`() = runTest {
    legacy.putBoolean(KEY_HAS_CONSENTED, true)
    legacy.putLong(PREF_FORWARD_TIME_MS, 45_000L)
    legacy.putFloat(PREF_PLAYBACK_SPEED, 1.5f)
    legacy.putDouble(KEY_FADE_OUT_DURATION, 12.0)
    legacy.putString(KEY_THEME, "dark")
    val store = testSettingsStore(InMemoryPreferencesDataStore(migration.migrate(emptyPreferences())))

    assertThat(PrivacySettingsImpl(store).observeHasEverConsented().first()).isTrue()
    val playback = PlaybackSettingsImpl(store)
    assertThat(playback.observeForwardTimeMs().first()).isEqualTo(45_000L)
    assertThat(playback.observePlaybackSpeed().first()).isEqualTo(1.5f)
    assertThat(SleepSettingsImpl(store).observeFadeOutDuration().first()).isEqualTo(12.seconds)
    assertThat(ThemeSettingsImpl(store, legacy).observeTheme().first()).isEqualTo(ThemeMode.DARK)
  }

  @Test
  fun `lists every key once`() {
    val keys = LegacyBooleanKeys + LegacyLongKeys + LegacyFloatKeys + LegacyDoubleKeys + LegacyStringKeys
    assertThat(keys.toSet().size).isEqualTo(keys.size)
  }
}

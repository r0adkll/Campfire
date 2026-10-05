// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.r0adkll.flatprefs.FlatPreferences
import com.r0adkll.flatprefs.FlatPreferencesMigration
import com.r0adkll.flatprefs.MutableFlatPreferences
import com.r0adkll.flatprefs.doubleKey
import com.r0adkll.flatprefs.longKey
import com.r0adkll.flatprefs.stringKey
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class DoubleBitsMigrationTest {

  /** Copies what multiplatform-settings left in SharedPreferences, as `SharedPreferencesMigration` would. */
  private class FakeSharedPreferencesMigration(private val values: Map<String, Any>) : FlatPreferencesMigration {
    var cleanedUp = false

    override fun shouldMigrate(current: FlatPreferences) = values.isNotEmpty()

    override fun migrate(prefs: MutableFlatPreferences) {
      values.forEach { (key, value) ->
        when (value) {
          is Long -> prefs[longKey(key)] = value
          is String -> prefs[stringKey(key)] = value
        }
      }
    }

    override fun cleanUp() {
      cleanedUp = true
    }
  }

  @Test
  fun `durations copied as long bits read back as doubles`() = runTest {
    val legacy = FakeSharedPreferencesMigration(
      mapOf(
        PREF_SYNC_INTERVAL_METERED to 90.0.toRawBits(),
        PREF_FORWARD_TIME_MS to 30_000L,
        KEY_THEME to "dark",
      ),
    )
    val file = newSettingsFile()
    val store = openSettingsStore(file, listOf(DoubleBitsMigration(legacy)))
    val settings = FlatPreferencesSettings(store, backgroundScope)

    assertThat(settings.getDouble(PREF_SYNC_INTERVAL_METERED, 0.0)).isEqualTo(90.0)
    // Longs that really are longs stay longs
    assertThat(settings.getLong(PREF_FORWARD_TIME_MS, 0L)).isEqualTo(30_000L)
    assertThat(settings.getString(KEY_THEME, "")).isEqualTo("dark")
    assertThat(readSettingsFile(file)[doubleKey(PREF_SYNC_INTERVAL_METERED)]).isEqualTo(90.0)
    assertThat(legacy.cleanedUp).isTrue()
  }

  @Test
  fun `a double the store already holds is left alone`() = runTest {
    val file = newSettingsFile()
    val store = openSettingsStore(file, emptyList())
    store.commit { it[doubleKey(KEY_SESSION_AGE)] = 3600.0 }

    val prefs = store.current.toMutable()
    DoubleBitsMigration(FakeSharedPreferencesMigration(emptyMap())).migrate(prefs)

    assertThat(prefs[doubleKey(KEY_SESSION_AGE)]).isEqualTo(3600.0)
  }
}

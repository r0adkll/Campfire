// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.r0adkll.flatprefs.FlatPreferences
import com.r0adkll.flatprefs.FlatPreferencesMigration
import com.r0adkll.flatprefs.MutableFlatPreferences
import com.r0adkll.flatprefs.stringKey
import java.io.IOException
import kotlin.test.Test

class SettingsStoreTest {

  @Test
  fun `durations kept as long bits come back as doubles`() {
    assertThat(restoreLegacyDouble(PREF_SYNC_INTERVAL_METERED, 90.0.toRawBits())).isEqualTo(90.0)
    assertThat(restoreLegacyDouble(KEY_SESSION_AGE, 3600.0.toRawBits())).isEqualTo(3600.0)
  }

  @Test
  fun `other values pass through unchanged`() {
    // A real long, and a duration that's somehow not a long
    assertThat(restoreLegacyDouble(PREF_FORWARD_TIME_MS, 30_000L)).isEqualTo(30_000L)
    assertThat(restoreLegacyDouble(KEY_THEME, "dark")).isEqualTo("dark")
    assertThat(restoreLegacyDouble(PREF_SYNC_INTERVAL_METERED, "90")).isInstanceOf<String>()
  }

  @Test
  fun `settings still read after the old ones fail to clean up`() {
    val failingCleanUp = object : FlatPreferencesMigration {
      override fun shouldMigrate(current: FlatPreferences) = KEY_THEME !in current

      override fun migrate(prefs: MutableFlatPreferences) {
        prefs[stringKey(KEY_THEME)] = "dark"
      }

      override fun cleanUp() = throw IOException("disk full")
    }

    val store = openSettingsStore(newSettingsFile(), listOf(failingCleanUp))

    assertThat(store[stringKey(KEY_THEME)]).isEqualTo("dark")
  }
}

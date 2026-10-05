// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import com.r0adkll.flatprefs.stringKey
import kotlin.test.Test

class FlatPreferencesDeviceBoundSettingsTest {

  @Test
  fun `restoring clears only the device-bound settings, on disk`() {
    val file = newSettingsFile()
    val store = openSettingsStore(file, migrations = emptyList())
    store.commitBlocking { prefs ->
      DeviceBoundSettingKeys.forEach { prefs[stringKey(it)] = "bound" }
      prefs[stringKey(KEY_THEME)] = "dark"
    }

    store.commitBlocking { it.clearDeviceBoundSettings() }

    assertThat(store.current.keys).containsOnly(KEY_THEME)
    assertThat(readSettingsFile(file).keys).containsOnly(KEY_THEME)
    assertThat(store[stringKey(KEY_THEME)]).isEqualTo("dark")
  }
}

// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.playback

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.campfire.core.audio.EqualizerPresets
import app.campfire.core.audio.EqualizerProfile
import app.campfire.settings.store.InMemoryPreferencesDataStore
import app.campfire.settings.store.testSettingsStore
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class EqualizerSettingsTest {

  @Test
  fun `equalizerProfile defaults and round-trips through storage`() = runTest {
    val settings = equalizerSettings()
    assertThat(settings.observeEqualizerProfile().first()).isEqualTo(EqualizerProfile())

    val profile = EqualizerProfile(
      enabled = true,
      presetId = EqualizerPresets.CUSTOM_ID,
      bandGainsDb = listOf(-4f, -3f, -1f, 0f, 2f, 4f, 4f, 2f, 0f, -1f),
      loudnessGainDb = 6.5f,
      bassBoost = 0.25f,
    )
    settings.setEqualizerProfile(profile)
    assertThat(settings.observeEqualizerProfile().first()).isEqualTo(profile)
  }

  @Test
  fun `corrupt profile strings fall back to the default`() = runTest {
    val backing = InMemoryPreferencesDataStore()
    val settings = EqualizerSettingsImpl(testSettingsStore(backing))

    listOf(
      "",
      "garbage",
      "1|voice_boost|1,2,3|6.0|0.25", // wrong band count
      "1|voice_boost|a,b,c,d,e,f,g,h,i,j|6.0|0.25", // non-numeric gains
      "1|voice_boost|0,0,0,0,0,0,0,0,0,0|nope|0.25", // non-numeric loudness
      "1||0,0,0,0,0,0,0,0,0,0|0.0|0.0", // empty preset id
      "1|flat|0,0,0,0,0,0,0,0,0,0|0.0", // missing field
    ).forEach { corrupt ->
      backing.edit { it[stringPreferencesKey(PREF_EQUALIZER_PROFILE)] = corrupt }
      assertThat(settings.observeEqualizerProfile().first()).isEqualTo(EqualizerProfile())
    }
  }

  private fun equalizerSettings(): EqualizerSettingsImpl =
    EqualizerSettingsImpl(testSettingsStore())
}

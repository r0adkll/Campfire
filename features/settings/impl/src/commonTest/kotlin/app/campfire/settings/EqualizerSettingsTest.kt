// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.audio.EqualizerPresets
import app.campfire.core.audio.EqualizerProfile
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class EqualizerSettingsTest {

  private val settingsScope = CoroutineScope(Dispatchers.Unconfined + Job())

  @Test
  fun `equalizerProfile defaults and round-trips through storage`() {
    val settings = equalizerSettings()
    assertThat(settings.equalizerProfile).isEqualTo(EqualizerProfile())

    val profile = EqualizerProfile(
      enabled = true,
      presetId = EqualizerPresets.CUSTOM_ID,
      bandGainsDb = listOf(-4f, -3f, -1f, 0f, 2f, 4f, 4f, 2f, 0f, -1f),
      loudnessGainDb = 6.5f,
      bassBoost = 0.25f,
    )
    settings.equalizerProfile = profile
    assertThat(settings.equalizerProfile).isEqualTo(profile)
  }

  @Test
  fun `corrupt profile strings fall back to the default`() {
    val backing = MapSettings()
    val settings = EqualizerSettingsImpl(backing, settingsScope)

    listOf(
      "",
      "garbage",
      "1|voice_boost|1,2,3|6.0|0.25", // wrong band count
      "1|voice_boost|a,b,c,d,e,f,g,h,i,j|6.0|0.25", // non-numeric gains
      "1|voice_boost|0,0,0,0,0,0,0,0,0,0|nope|0.25", // non-numeric loudness
      "1||0,0,0,0,0,0,0,0,0,0|0.0|0.0", // empty preset id
      "1|flat|0,0,0,0,0,0,0,0,0,0|0.0", // missing field
    ).forEach { corrupt ->
      backing.putString(PREF_EQUALIZER_PROFILE, corrupt)
      assertThat(settings.equalizerProfile).isEqualTo(EqualizerProfile())
    }
  }

  @Test
  fun `itemEqualizerProfiles round-trips and drops corrupt entries`() {
    val settings = equalizerSettings()
    assertThat(settings.itemEqualizerProfiles).isEmpty()

    val profiles = mapOf(
      "li_abc123" to EqualizerProfile(enabled = true, presetId = EqualizerPresets.BASS_BOOST_ID),
      "li_def456" to EqualizerProfile(bandGainsDb = listOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f)),
    )
    settings.itemEqualizerProfiles = profiles
    assertThat(settings.itemEqualizerProfiles).isEqualTo(profiles)

    settings.itemEqualizerProfiles = emptyMap()
    assertThat(settings.itemEqualizerProfiles).isEmpty()
  }

  @Test
  fun `equalizerProfileFor falls back to the global profile without an override`() {
    val settings = equalizerSettings()
    val global = EqualizerProfile(enabled = true, presetId = EqualizerPresets.WARM_ID)
    val override = EqualizerProfile(enabled = true, presetId = EqualizerPresets.VOICE_BOOST_ID)
    settings.equalizerProfile = global
    settings.itemEqualizerProfiles = mapOf("li_abc123" to override)

    assertThat(settings.equalizerProfileFor("li_abc123")).isEqualTo(override)
    assertThat(settings.equalizerProfileFor("li_other")).isEqualTo(global)
    assertThat(settings.equalizerProfileFor(null)).isEqualTo(global)
  }

  @Test
  fun `setEqualizerProfileFor writes the override when enabled and the global otherwise`() {
    val settings = equalizerSettings()
    val initial = EqualizerProfile(enabled = true)
    settings.itemEqualizerProfiles = mapOf("li_abc123" to initial)

    val updated = initial.copy(loudnessGainDb = 3f)
    settings.setEqualizerProfileFor("li_abc123", updated)
    assertThat(settings.itemEqualizerProfiles).isEqualTo(mapOf("li_abc123" to updated))
    assertThat(settings.equalizerProfile).isEqualTo(EqualizerProfile())

    settings.setEqualizerProfileFor("li_other", updated)
    assertThat(settings.equalizerProfile).isEqualTo(updated)
    assertThat(settings.itemEqualizerProfiles).isEqualTo(mapOf("li_abc123" to updated))
  }

  private fun equalizerSettings(): EqualizerSettingsImpl =
    EqualizerSettingsImpl(MapSettings(), settingsScope)
}

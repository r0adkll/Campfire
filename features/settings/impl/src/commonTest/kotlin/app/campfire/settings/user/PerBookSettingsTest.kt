// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.user

import app.campfire.core.audio.EqualizerPresets
import app.campfire.core.audio.EqualizerProfile
import app.campfire.settings.playback.EqualizerSettingsImpl
import app.campfire.settings.playback.PlaybackSettingsImpl
import app.campfire.settings.store.testSettingsStore
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class PerBookSettingsTest {

  /** The app-wide settings live in the app's store; the per-book ones in the account's. */
  private val playback = PlaybackSettingsImpl(testSettingsStore())
  private val equalizer = EqualizerSettingsImpl(testSettingsStore())
  private val settings = PerBookSettingsImpl(testSettingsStore(), playback, equalizer)

  @Test
  fun `itemPlaybackSpeeds defaults to empty and round-trips through storage`() = runTest {
    assertThat(settings.observeItemPlaybackSpeeds().first()).isEmpty()

    val speeds = mapOf("li_abc123" to 1.5f, "li_def456" to 0.75f)
    settings.setItemPlaybackSpeeds(speeds)
    assertThat(settings.observeItemPlaybackSpeeds().first()).isEqualTo(speeds)

    settings.setItemPlaybackSpeeds(emptyMap())
    assertThat(settings.observeItemPlaybackSpeeds().first()).isEmpty()
  }

  @Test
  fun `playbackSpeedFor falls back to the app-wide speed without the book's own`() = runTest {
    playback.setPlaybackSpeed(1.25f)
    settings.setItemPlaybackSpeeds(mapOf("li_abc123" to 2f))

    assertThat(settings.playbackSpeedFor("li_abc123")).isEqualTo(2f)
    assertThat(settings.playbackSpeedFor("li_other")).isEqualTo(1.25f)
    assertThat(settings.playbackSpeedFor(null)).isEqualTo(1.25f)
  }

  @Test
  fun `setPlaybackSpeedFor writes the book's own speed when it has one and the app-wide otherwise`() = runTest {
    playback.setPlaybackSpeed(1f)
    settings.setItemPlaybackSpeeds(mapOf("li_abc123" to 1.5f))

    // A book with its own speed: only its entry changes
    settings.setPlaybackSpeedFor("li_abc123", 1.75f)
    assertThat(settings.observeItemPlaybackSpeeds().first()).isEqualTo(mapOf("li_abc123" to 1.75f))
    assertThat(playback.observePlaybackSpeed().first()).isEqualTo(1f)

    // A book without one: the app-wide speed changes, no entry is created
    settings.setPlaybackSpeedFor("li_other", 1.2f)
    assertThat(playback.observePlaybackSpeed().first()).isEqualTo(1.2f)
    assertThat(settings.observeItemPlaybackSpeeds().first()).isEqualTo(mapOf("li_abc123" to 1.75f))

    // No book at all: the app-wide speed changes
    settings.setPlaybackSpeedFor(null, 0.9f)
    assertThat(playback.observePlaybackSpeed().first()).isEqualTo(0.9f)
  }

  @Test
  fun `itemEqualizerProfiles round-trips through storage`() = runTest {
    assertThat(settings.observeItemEqualizerProfiles().first()).isEmpty()

    val profiles = mapOf(
      "li_abc123" to EqualizerProfile(enabled = true, presetId = EqualizerPresets.BASS_BOOST_ID),
      "li_def456" to EqualizerProfile(bandGainsDb = listOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f)),
    )
    settings.setItemEqualizerProfiles(profiles)
    assertThat(settings.observeItemEqualizerProfiles().first()).isEqualTo(profiles)

    settings.setItemEqualizerProfiles(emptyMap())
    assertThat(settings.observeItemEqualizerProfiles().first()).isEmpty()
  }

  @Test
  fun `equalizerProfileFor falls back to the app-wide profile without the book's own`() = runTest {
    val global = EqualizerProfile(enabled = true, presetId = EqualizerPresets.WARM_ID)
    val own = EqualizerProfile(enabled = true, presetId = EqualizerPresets.VOICE_BOOST_ID)
    equalizer.setEqualizerProfile(global)
    settings.setItemEqualizerProfiles(mapOf("li_abc123" to own))

    assertThat(settings.equalizerProfileFor("li_abc123")).isEqualTo(own)
    assertThat(settings.equalizerProfileFor("li_other")).isEqualTo(global)
    assertThat(settings.equalizerProfileFor(null)).isEqualTo(global)
  }

  @Test
  fun `setEqualizerProfileFor writes the book's own profile when it has one and the app-wide otherwise`() = runTest {
    val initial = EqualizerProfile(enabled = true)
    settings.setItemEqualizerProfiles(mapOf("li_abc123" to initial))

    val updated = initial.copy(loudnessGainDb = 3f)
    settings.setEqualizerProfileFor("li_abc123", updated)
    assertThat(settings.observeItemEqualizerProfiles().first()).isEqualTo(mapOf("li_abc123" to updated))
    assertThat(equalizer.observeEqualizerProfile().first()).isEqualTo(EqualizerProfile())

    settings.setEqualizerProfileFor("li_other", updated)
    assertThat(equalizer.observeEqualizerProfile().first()).isEqualTo(updated)
    assertThat(settings.observeItemEqualizerProfiles().first()).isEqualTo(mapOf("li_abc123" to updated))
  }
}

// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.playback

import app.campfire.settings.store.testSettingsStore
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class ItemPlaybackSpeedsTest {

  @Test
  fun `itemPlaybackSpeeds defaults to empty and round-trips through storage`() = runTest {
    val settings = playbackSettings()
    assertThat(settings.observeItemPlaybackSpeeds().first()).isEmpty()

    val speeds = mapOf("li_abc123" to 1.5f, "li_def456" to 0.75f)
    settings.setItemPlaybackSpeeds(speeds)
    assertThat(settings.observeItemPlaybackSpeeds().first()).isEqualTo(speeds)

    settings.setItemPlaybackSpeeds(emptyMap())
    assertThat(settings.observeItemPlaybackSpeeds().first()).isEmpty()
  }

  @Test
  fun `playbackSpeedFor falls back to the global speed without an override`() = runTest {
    val settings = playbackSettings()
    settings.setPlaybackSpeed(1.25f)
    settings.setItemPlaybackSpeeds(mapOf("li_abc123" to 2f))

    assertThat(settings.playbackSpeedFor("li_abc123")).isEqualTo(2f)
    assertThat(settings.playbackSpeedFor("li_other")).isEqualTo(1.25f)
    assertThat(settings.playbackSpeedFor(null)).isEqualTo(1.25f)
  }

  @Test
  fun `setPlaybackSpeedFor writes the override when enabled and the global otherwise`() = runTest {
    val settings = playbackSettings()
    settings.setPlaybackSpeed(1f)
    settings.setItemPlaybackSpeeds(mapOf("li_abc123" to 1.5f))

    // Item with an override enabled: only its entry changes
    settings.setPlaybackSpeedFor("li_abc123", 1.75f)
    assertThat(settings.observeItemPlaybackSpeeds().first()).isEqualTo(mapOf("li_abc123" to 1.75f))
    assertThat(settings.observePlaybackSpeed().first()).isEqualTo(1f)

    // Item without an override: the global changes, no entry is created
    settings.setPlaybackSpeedFor("li_other", 1.2f)
    assertThat(settings.observePlaybackSpeed().first()).isEqualTo(1.2f)
    assertThat(settings.observeItemPlaybackSpeeds().first()).isEqualTo(mapOf("li_abc123" to 1.75f))

    // No item at all: the global changes
    settings.setPlaybackSpeedFor(null, 0.9f)
    assertThat(settings.observePlaybackSpeed().first()).isEqualTo(0.9f)
  }

  private fun playbackSettings(): PlaybackSettingsImpl =
    PlaybackSettingsImpl(testSettingsStore())
}

// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class ItemPlaybackSpeedsTest {

  private val settingsScope = CoroutineScope(Dispatchers.Unconfined + Job())

  @Test
  fun `itemPlaybackSpeeds defaults to empty and round-trips through storage`() {
    val settings = playbackSettings()
    assertThat(settings.itemPlaybackSpeeds).isEmpty()

    val speeds = mapOf("li_abc123" to 1.5f, "li_def456" to 0.75f)
    settings.itemPlaybackSpeeds = speeds
    assertThat(settings.itemPlaybackSpeeds).isEqualTo(speeds)

    settings.itemPlaybackSpeeds = emptyMap()
    assertThat(settings.itemPlaybackSpeeds).isEmpty()
  }

  @Test
  fun `playbackSpeedFor falls back to the global speed without an override`() {
    val settings = playbackSettings()
    settings.playbackSpeed = 1.25f
    settings.itemPlaybackSpeeds = mapOf("li_abc123" to 2f)

    assertThat(settings.playbackSpeedFor("li_abc123")).isEqualTo(2f)
    assertThat(settings.playbackSpeedFor("li_other")).isEqualTo(1.25f)
    assertThat(settings.playbackSpeedFor(null)).isEqualTo(1.25f)
  }

  @Test
  fun `setPlaybackSpeedFor writes the override when enabled and the global otherwise`() {
    val settings = playbackSettings()
    settings.playbackSpeed = 1f
    settings.itemPlaybackSpeeds = mapOf("li_abc123" to 1.5f)

    // Item with an override enabled: only its entry changes
    settings.setPlaybackSpeedFor("li_abc123", 1.75f)
    assertThat(settings.itemPlaybackSpeeds).isEqualTo(mapOf("li_abc123" to 1.75f))
    assertThat(settings.playbackSpeed).isEqualTo(1f)

    // Item without an override: the global changes, no entry is created
    settings.setPlaybackSpeedFor("li_other", 1.2f)
    assertThat(settings.playbackSpeed).isEqualTo(1.2f)
    assertThat(settings.itemPlaybackSpeeds).isEqualTo(mapOf("li_abc123" to 1.75f))

    // No item at all: the global changes
    settings.setPlaybackSpeedFor(null, 0.9f)
    assertThat(settings.playbackSpeed).isEqualTo(0.9f)
  }

  private fun playbackSettings(): PlaybackSettingsImpl =
    PlaybackSettingsImpl(MapSettings(), settingsScope)
}

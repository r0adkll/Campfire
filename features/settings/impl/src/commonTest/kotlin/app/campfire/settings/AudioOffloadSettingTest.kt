// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class AudioOffloadSettingTest {

  private val settingsScope = CoroutineScope(Dispatchers.Unconfined + Job())

  @Test
  fun `audio offload defaults to on and persists when turned off`() {
    val storage = MapSettings()
    val settings = PlaybackSettingsImpl(storage, settingsScope)
    assertThat(settings.audioOffloadEnabled).isTrue()

    settings.audioOffloadEnabled = false

    assertThat(PlaybackSettingsImpl(storage, settingsScope).audioOffloadEnabled).isFalse()
    assertThat(settings.observeAudioOffloadEnabled().value).isFalse()
  }
}

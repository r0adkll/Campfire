// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.store

import app.campfire.settings.app.KEY_ANALYTICS_ID
import app.campfire.settings.app.KEY_CURRENT_USER_ID
import app.campfire.settings.app.KEY_DEVICE_ID
import app.campfire.settings.playback.PREF_OUTPUT_DEVICE
import app.campfire.settings.playback.PREF_PENDING_RESUME_REWIND
import app.campfire.settings.playback.PREF_PLAYBACK_SPEED
import app.campfire.settings.theme.KEY_THEME
import assertk.assertThat
import assertk.assertions.containsOnly
import com.russhwolf.settings.MapSettings
import kotlin.test.Test

class DeviceBoundSettingsTest {

  @Test
  fun `clearing a restore keeps preferences but drops device bound settings`() {
    val settings = MapSettings(
      KEY_DEVICE_ID to "device",
      KEY_ANALYTICS_ID to "analytics",
      KEY_CURRENT_USER_ID to "user",
      PREF_OUTPUT_DEVICE to "Speakers",
      PREF_PENDING_RESUME_REWIND to 5_000L,
      KEY_THEME to "dark",
      PREF_PLAYBACK_SPEED to 1.5f,
    )

    settings.clearDeviceBoundSettings()

    assertThat(settings.keys).containsOnly(KEY_THEME, PREF_PLAYBACK_SPEED)
  }
}

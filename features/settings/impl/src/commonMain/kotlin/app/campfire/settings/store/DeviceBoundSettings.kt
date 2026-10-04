// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.store

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import app.campfire.settings.app.KEY_ANALYTICS_ID
import app.campfire.settings.app.KEY_CURRENT_USER_ID
import app.campfire.settings.app.KEY_DEVICE_ID
import app.campfire.settings.playback.PREF_OUTPUT_DEVICE
import app.campfire.settings.playback.PREF_PENDING_RESUME_REWIND
import com.russhwolf.settings.Settings

/**
 * Settings that belong to this install on this device, so they must not come back with a platform
 * backup restored onto another device (or a fresh install):
 *
 * - The device id identifies this device to the server; two devices sharing one are merged there.
 * - The analytics id is per install.
 * - The audio output device and a pending resume rewind only make sense where they were set.
 * - The current user has no stored token after a restore, since tokens are never backed up.
 */
internal val DeviceBoundSettingKeys = setOf(
  KEY_DEVICE_ID,
  KEY_ANALYTICS_ID,
  KEY_CURRENT_USER_ID,
  PREF_OUTPUT_DEVICE,
  PREF_PENDING_RESUME_REWIND,
)

internal fun Settings.clearDeviceBoundSettings() {
  DeviceBoundSettingKeys.forEach(::remove)
}

internal fun MutablePreferences.clearDeviceBoundSettings() {
  // Keys match by name, whatever type the value was stored as; every device-bound setting is a string
  DeviceBoundSettingKeys.forEach { remove(stringPreferencesKey(it)) }
}

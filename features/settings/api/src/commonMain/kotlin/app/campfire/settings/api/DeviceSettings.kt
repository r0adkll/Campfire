// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import app.campfire.core.model.UserId
import kotlinx.coroutines.flow.StateFlow

/**
 * Identity that belongs to this install on this device.
 */
interface DeviceSettings {

  var deviceId: String

  var analyticsId: String

  var currentUserId: UserId?
  fun observeCurrentUserId(): StateFlow<UserId?>
}

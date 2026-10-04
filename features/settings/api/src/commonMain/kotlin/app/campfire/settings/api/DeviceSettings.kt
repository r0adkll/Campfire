// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import app.campfire.core.model.UserId
import kotlinx.coroutines.flow.Flow

/**
 * Identity that belongs to this install on this device.
 */
interface DeviceSettings {

  /** This device's id, created the first time it's asked for. */
  suspend fun deviceId(): String

  /** This install's analytics id, created the first time it's asked for. */
  suspend fun analyticsId(): String

  fun setCurrentUserId(value: UserId?)
  fun observeCurrentUserId(): Flow<UserId?>
}

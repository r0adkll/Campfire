// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import app.campfire.core.model.UserId

/** The settings files each account keeps for itself, one per account. */
interface UserSettingsStores {

  /** Deletes [userId]'s settings, for when their account is removed from this device. */
  suspend fun delete(userId: UserId)
}

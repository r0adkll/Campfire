// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import app.campfire.core.model.UserId

/** The settings each account keeps for itself, such as how its libraries are sorted and laid out. */
interface UserSettingsStore {

  /** Removes [userId]'s settings, for when their account is removed from this device. */
  fun clear(userId: UserId)
}

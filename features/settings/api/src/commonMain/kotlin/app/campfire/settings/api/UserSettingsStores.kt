// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import app.campfire.core.model.UserId

/**
 * The settings each account keeps for itself, such as how its libraries are sorted and laid out, stored in a
 * file per account. Signed out, a temporary in-memory set stands in.
 */
interface UserSettingsStores {

  /** Starts reading [userId]'s settings in the background, ahead of their user graph needing them. */
  fun startLoading(userId: UserId)

  /** Returns once the settings for [userId] are in memory, or the signed-out settings when it's null. */
  suspend fun load(userId: UserId?)

  /** Deletes [userId]'s settings, for when their account is removed from this device. */
  suspend fun delete(userId: UserId)
}

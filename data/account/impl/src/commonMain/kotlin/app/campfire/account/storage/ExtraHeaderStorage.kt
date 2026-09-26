// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.storage

import app.campfire.core.model.UserId
import kotlinx.coroutines.flow.Flow

interface ExtraHeaderStorage {
  suspend fun get(userId: UserId): Map<String, String>?
  suspend fun put(userId: UserId, headers: Map<String, String>)
  suspend fun remove(userId: UserId)

  /** The headers for [userId], re-emitting after every [put] or [remove]. */
  fun observe(userId: UserId): Flow<Map<String, String>>
}

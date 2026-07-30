// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.storage

import app.campfire.core.model.UserId

interface ExtraHeaderStorage {
  suspend fun get(userId: UserId): Map<String, String>?
  suspend fun put(userId: UserId, headers: Map<String, String>)
  suspend fun remove(userId: UserId)
}

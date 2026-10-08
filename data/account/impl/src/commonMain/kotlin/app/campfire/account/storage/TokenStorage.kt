// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.storage

import app.campfire.account.api.AbsToken
import app.campfire.core.model.UserId

/**
 * Interface for storing and fetching tokens from settings
 */
interface TokenStorage {

  suspend fun get(userId: UserId): AbsToken?

  /**
   * Whether a token is stored for [userId]. Unlike [get] this doesn't read the token, so it costs
   * no decryption, and a stored token that no longer decrypts still counts.
   */
  suspend fun has(userId: UserId): Boolean
  suspend fun put(userId: UserId, token: AbsToken)
  suspend fun remove(userId: UserId)
}

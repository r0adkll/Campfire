// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.api

import app.campfire.core.model.UserId
import kotlinx.coroutines.flow.Flow

/**
 * A copy of each account's sign-in, kept where it survives the app being reinstalled or moved to a
 * new device (Block Store in Google Play builds), so the account can be signed back into without
 * a password. Where there's nowhere to keep it, [isAvailable] is false and nothing is kept.
 */
interface TokenBackup {

  val isAvailable: Boolean

  /**
   * Keep [account], replacing what was kept for its user. Call again whenever its token rotates:
   * refresh tokens are single use, so an older copy can't sign back in.
   */
  suspend fun put(account: BackedUpAccount)

  suspend fun remove(userId: UserId)

  /**
   * Remove every kept account
   */
  suspend fun clear()

  /**
   * The accounts kept by this or a previous install
   */
  suspend fun getAll(): List<BackedUpAccount>
}

data class BackedUpAccount(
  val serverUrl: String,
  val serverName: String,
  val userId: UserId,
  val token: AbsToken,
  val extraHeaders: Map<String, String>,
)

/**
 * Signs accounts kept in [TokenBackup] back in after a reinstall.
 */
interface BackedUpAccountRestorer {

  /**
   * The users currently being signed back in
   */
  val restoring: Flow<Set<UserId>>

  /**
   * Sign every kept account back in that isn't already, once per launch. The most recently used
   * one becomes the active account. Runs past the caller, since signing in replaces its UI.
   */
  fun restore()
}

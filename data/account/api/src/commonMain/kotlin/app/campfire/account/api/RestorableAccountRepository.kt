// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.api

import app.campfire.core.model.UserId
import kotlinx.coroutines.flow.Flow

/**
 * Accounts that were signed in before the app's data was restored from a platform backup (e.g. a
 * reinstall), but that haven't been signed back into yet. Tokens are never part of a backup, so
 * these only carry enough to prefill the login form.
 */
interface RestorableAccountRepository {

  /**
   * Observe the accounts waiting to be signed back into, the most recently used first
   */
  fun observeRestorableAccounts(): Flow<List<RestorableAccount>>

  /**
   * Stop offering [account] for sign in
   */
  suspend fun dismiss(account: RestorableAccount)
}

data class RestorableAccount(
  val serverUrl: String,
  val serverName: String,
  val userId: UserId,
  val userName: String,
)

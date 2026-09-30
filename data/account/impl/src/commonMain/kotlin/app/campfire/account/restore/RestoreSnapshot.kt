// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.restore

import app.campfire.account.api.RestorableAccount
import app.campfire.core.model.Server
import app.campfire.core.model.UserId
import kotlinx.serialization.Serializable

/**
 * What survives a platform backup of the account list.
 *
 * @property live the accounts currently in the database, mirrored on every change
 * @property pending accounts from a previous install that haven't been signed back into
 * @property lastUserId the most recently active account, used to order [pending]
 */
@Serializable
internal data class RestoreSnapshot(
  val live: List<RestoreEntry> = emptyList(),
  val pending: List<RestoreEntry> = emptyList(),
  val lastUserId: UserId? = null,
) {

  /**
   * Replace [live] with the accounts in the database. Anything previously known that the database
   * no longer has — every account right after a restore, since the database isn't backed up — is
   * carried into [pending] until it's signed back into or dismissed. Logging out must [forget] an
   * account before deleting it, or it would be carried over too.
   */
  fun reconcile(accounts: List<RestoreEntry>, currentUserId: UserId?): RestoreSnapshot {
    val missing = (pending + live)
      .filterNot { entry -> accounts.any { it.isSameAccount(entry) } }
      .distinctBy { it.userId to it.host }

    return RestoreSnapshot(
      live = accounts,
      pending = missing,
      lastUserId = currentUserId ?: lastUserId,
    )
  }

  fun forget(userId: UserId): RestoreSnapshot = copy(
    live = live.filterNot { it.userId == userId },
    pending = pending.filterNot { it.userId == userId },
  )

  fun dismiss(account: RestorableAccount): RestoreSnapshot = copy(
    pending = pending.filterNot { it.isSameAccount(RestoreEntry(account)) },
  )

  fun restorableAccounts(): List<RestorableAccount> = pending
    .sortedByDescending { it.userId == lastUserId }
    .map { it.asRestorableAccount() }
}

@Serializable
internal data class RestoreEntry(
  val serverUrl: String,
  val serverName: String,
  val userId: UserId,
  val userName: String,
) {

  constructor(server: Server) : this(
    serverUrl = server.url,
    serverName = server.name,
    userId = server.user.id,
    userName = server.user.name,
  )

  constructor(account: RestorableAccount) : this(
    serverUrl = account.serverUrl,
    serverName = account.serverName,
    userId = account.userId,
    userName = account.userName,
  )

  // Signing back in can land on a different scheme or path for the same server (the login form
  // probes http and https), so accounts are matched by host rather than the full URL.
  val host: String
    get() = serverUrl
      .substringAfter("://")
      .substringBefore('/')
      .lowercase()

  fun isSameAccount(other: RestoreEntry): Boolean =
    userId == other.userId && host == other.host

  fun asRestorableAccount() = RestorableAccount(
    serverUrl = serverUrl,
    serverName = serverName,
    userId = userId,
    userName = userName,
  )
}

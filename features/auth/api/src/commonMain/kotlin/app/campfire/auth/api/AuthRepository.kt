// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.api

import app.campfire.account.api.BackedUpAccount
import app.campfire.auth.api.model.ServerStatus
import app.campfire.core.model.NetworkSettings
import app.campfire.core.model.User
import app.campfire.core.model.UserId

interface AuthRepository {

  suspend fun status(
    serverUrl: String,
    networkSettings: NetworkSettings? = null,
  ): Result<ServerStatus>

  /**
   * Check that [serverUrl] can take a new account. Fails with [AuthException.ServerAlreadyAdded]
   * when an account on that server is already on the device: Campfire keeps one account per
   * server. Signing in without a `userId`, and [restore], check this themselves; call it to stop
   * earlier, e.g. before sending the user off to an OpenID provider.
   */
  suspend fun checkServerAvailable(serverUrl: String): Result<Unit>

  suspend fun authenticate(
    serverUrl: String,
    serverName: String,
    username: String,
    password: String,
    userId: UserId? = null,
    networkSettings: NetworkSettings? = null,
  ): Result<Unit>

  suspend fun authenticate(
    serverUrl: String,
    serverName: String,
    codeVerifier: String,
    code: String,
    state: String,
    userId: UserId? = null,
    networkSettings: NetworkSettings? = null,
  ): Result<Unit>

  /**
   * Sign a kept [account] back in without a password, by trading its refresh token for a new
   * sign-in. Fails with [AuthException.InvalidCredentials] when the server no longer accepts the
   * token.
   *
   * @param activate whether to switch to the account once it's back
   * @return the signed back in user
   */
  suspend fun restore(account: BackedUpAccount, activate: Boolean): Result<User>

  suspend fun getNetworkSettings(userId: UserId): NetworkSettings?
}

package app.campfire.account.api

import app.campfire.core.model.Server

/**
 * The interface by which the entire application accesses all the stored/authorized accounts and servicers
 * that have been logged into
 */
interface AccountManager {

  /**
   * Store the access token for a given [Server]
   * @param server the [Server] to store the token for
   * @param token the token to store
   */
  suspend fun setToken(server: Server, token: String)

  /**
   * Get the access token for a given account to use to authenticate
   * requests on behalf of the account.
   * @param server the [Server] of the token to fetch
   */
  suspend fun getToken(server: Server): String?
}

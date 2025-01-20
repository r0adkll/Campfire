package app.campfire.account.api

import app.campfire.core.model.Server
import app.campfire.core.model.User
import app.campfire.core.model.UserId

/**
 * The interface by which the entire application accesses all the stored/authorized accounts and servicers
 * that have been logged into
 */
interface AccountManager {

  /**
   * Add a new account to the app, storing its access token in secure storage
   * and setting the account as the current account
   *
   * @param serverUrl the url of the server the account is from
   * @param token the access token the account will use to access the server
   * @param user the user object representing the new account
   */
  suspend fun addAccount(
    serverUrl: String,
    token: String,
    user: User,
  )

  /**
   * Switch the current account/session to a new user
   * @param user the user to switch the account to
   */
  suspend fun switchAccount(user: User)

  /**
   * Logout of an account, if exists
   * @param server the account and server to logout of
   */
  suspend fun logout(server: Server)

  /**
   * Get the access token for a given account to use to authenticate
   * requests on behalf of the account.
   * @param userId the id of the user to fetch a token for
   */
  suspend fun getToken(userId: UserId): String?
}

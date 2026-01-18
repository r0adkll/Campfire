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
   * @param accessToken the access token the account will use to access the server
   * @param refreshToken the refresh token the account will use to refresh the access token
   * @param user the user object representing the new account
   */
  suspend fun addAccount(
    serverUrl: String,
    accessToken: String,
    refreshToken: String?,
    extraHeaders: Map<String, String>?,
    user: User,
  )

  /**
   * Invalidate an account to require re-authentication by the user
   *
   */
  suspend fun invalidateAccount(user: User)

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
  suspend fun getToken(userId: UserId): AbsToken?

  /**
   * Get the legacy auth token for a given account to use to migrate
   * to the new access/refresh bearer token system. \
   *
   * @param userId the id of the user to fetch a token for
   * @return the legacy auth token, or null if none exists
   */
  suspend fun getLegacyToken(userId: UserId): String?

  /**
   * Delete the legacy token for a user
   */
  suspend fun removeLegacyToken(userId: UserId)

  /**
   * Update the set of tokens for a given user
   */
  suspend fun updateToken(userId: UserId, newToken: AbsToken)

  /**
   * Get a set of extra headers the User configured during Authentication
   * to send in every network request.
   * @param userId the id of the user to fetch extra headers for
   * @return a map of extra headers to send
   */
  suspend fun getExtraHeaders(userId: UserId): Map<String, String>?
}

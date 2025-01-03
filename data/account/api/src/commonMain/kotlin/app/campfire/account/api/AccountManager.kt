package app.campfire.account.api

/**
 * The interface by which the entire application accesses all the stored/authorized accounts and servicers
 * that have been logged into
 *
 * FIXME: We are keying tokens to a server url in this class(es) and this is incorrect as feasibly a user
 *  could log into multiple accounts under the same server (for some reason) and so we should compose the
 *  login key out of both the server url and user id.
 */
interface AccountManager {

  /**
   * Store the access token for a given server url
   * @param serverUrl the server url to store the token for
   * @param token the token to store
   */
  suspend fun setToken(serverUrl: String, token: String)

  /**
   * Get the access token for a given account to use to authenticate
   * requests on behalf of the account.
   * @param serverUrl the server url of the token to fetch
   */
  suspend fun getToken(serverUrl: String): String?
}

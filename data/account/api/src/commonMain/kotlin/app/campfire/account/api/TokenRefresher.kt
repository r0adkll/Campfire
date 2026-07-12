package app.campfire.account.api

import app.campfire.core.model.UserId

/**
 * Ensures the stored access/refresh token pair for an account is fresh, persisting any rotated
 * pair via [AccountManager]. Used by transports that authenticate with the raw access token
 * (e.g. the realtime socket) when the server rejects the stored token as expired.
 *
 * Implementations must not race the HTTP client's own bearer-refresh machinery — the HTTP
 * layer is the single authority for rotating the (single-use) refresh token.
 */
interface TokenRefresher {

  /**
   * Refresh the token pair for [userId] against [serverUrl].
   *
   * @param staleAccessToken the access token the caller just failed to authenticate with. If
   * the stored token already differs (the HTTP layer refreshed it in the meantime), the stored
   * token is returned without triggering another refresh.
   * @return a fresh token pair, or null if the refresh failed. A refresh rejected as
   * unauthorized invalidates the account via [AccountManager.invalidateAccount].
   */
  suspend fun refresh(userId: UserId, serverUrl: String, staleAccessToken: String?): AbsToken?
}

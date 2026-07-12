package app.campfire.network

import app.campfire.account.api.AbsToken
import app.campfire.account.api.AccountManager
import app.campfire.account.api.TokenRefresher
import app.campfire.account.api.UserSessionManager
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.UserId
import app.campfire.core.session.UserSession
import app.campfire.network.di.UserClient
import com.r0adkll.kimchi.annotations.ContributesBinding
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.http.isSuccess
import kotlinx.io.IOException
import me.tatarka.inject.annotations.Inject

/**
 * A [TokenRefresher] that deliberately does NOT call `/auth/refresh` itself. Ktor's Auth plugin
 * caches the bearer pair internally and single-flights its own refresh, so a second refresher
 * rotating the (single-use) refresh token out-of-band would race it and orphan the plugin's
 * cached copy, forcing a re-login. Instead we fire a cheap authenticated request through the
 * shared user client: if the access token is stale the plugin observes the 401, refreshes
 * through its own machinery, and persists the rotated pair via [AccountManager.updateToken] —
 * which we then read back.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class UserClientTokenRefresher(
  @UserClient private val httpClient: HttpClient,
  private val accountManager: AccountManager,
  private val userSessionManager: UserSessionManager,
) : TokenRefresher {

  override suspend fun refresh(userId: UserId, serverUrl: String, staleAccessToken: String?): AbsToken? {
    // The user client authenticates as the current session's user — poking it on behalf of
    // anyone else would refresh the wrong account's tokens.
    val currentUserId = (userSessionManager.current as? UserSession.LoggedIn)?.user?.id
    if (currentUserId != userId) {
      bark(LogPriority.WARN) { "Declining token refresh for a non-current user" }
      return null
    }

    // If the stored token already moved past the one the caller was rejected with, the Auth
    // plugin refreshed in the meantime — no need to poke it again.
    val stored = accountManager.getToken(userId)
    if (stored != null && staleAccessToken != null && stored.accessToken != staleAccessToken) {
      return stored
    }

    val response = try {
      httpClient.post("${cleanServerUrl(serverUrl)}/api/authorize")
    } catch (e: IOException) {
      bark(LogPriority.ERROR, throwable = e) { "Token-refreshing authorize request failed to send" }
      return null
    }

    if (!response.status.isSuccess()) {
      // A 401 here means the Auth plugin already tried to refresh and failed (and, for a dead
      // refresh token, already invalidated the account) — nothing more we can do.
      bark(LogPriority.ERROR) { "[${response.status}] Token-refreshing authorize request failed" }
      return null
    }

    return accountManager.getToken(userId)
  }
}

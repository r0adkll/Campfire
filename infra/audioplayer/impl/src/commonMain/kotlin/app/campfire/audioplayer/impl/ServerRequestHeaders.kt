// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl

import app.campfire.account.api.AccountManager
import app.campfire.account.api.UserSessionManager
import app.campfire.core.model.UserId
import app.campfire.core.session.UserSession
import dev.zacsweers.metro.Inject
import io.ktor.http.HttpHeaders
import io.ktor.http.URLParserException
import io.ktor.http.Url

/**
 * The credentials the user client sends, for requests made outside it (the FFmpeg and AVPlayer
 * engines, the desktop Now Playing cover loader): the bearer token plus the account's extra headers, such as a
 * Cloudflare Access service token. Without the extra headers a server behind an access proxy
 * answers with a redirect to its login page.
 *
 * Like the user client, credentials only go to the logged-in user's own server, so a URL on any
 * other host (third-party cover art, say) gets none.
 */
@Inject
class ServerRequestHeaders(
  private val accountManager: AccountManager,
  private val userSessionManager: UserSessionManager,
) {

  /** Headers authenticating a request for [url] as [userId]; empty when [url] isn't on that user's server. */
  suspend fun forUrl(userId: UserId, url: String): Map<String, String> {
    val user = (userSessionManager.current as? UserSession.LoggedIn)?.user
    if (user == null || user.id != userId || !isOnServer(url, user.serverUrl)) return emptyMap()

    val token = accountManager.getToken(userId)?.accessToken
    return buildMap {
      accountManager.getExtraHeaders(userId)?.let(::putAll)
      if (!token.isNullOrEmpty()) put(HttpHeaders.Authorization, "Bearer $token")
    }
  }

  /** Compares host and port, as the user client does; a server URL without a scheme is https. */
  private fun isOnServer(url: String, serverUrl: String): Boolean {
    val server = serverUrl.takeIf { it.startsWith("http://") || it.startsWith("https://") }
      ?: "https://$serverUrl"
    return try {
      val requested = Url(url)
      val expected = Url(server)
      requested.host.equals(expected.host, ignoreCase = true) && requested.port == expected.port
    } catch (e: URLParserException) {
      false
    }
  }
}

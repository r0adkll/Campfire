package app.campfire.network

import app.campfire.account.api.AbsToken
import io.ktor.client.plugins.auth.providers.BearerTokens
import kotlinx.serialization.Serializable

fun AbsToken.asBearerTokens(): BearerTokens = BearerTokens(
  accessToken = accessToken,
  refreshToken = refreshToken,
)

/**
 * `/auth/refresh` responds with the same envelope as `/login`, but we only need the rotated
 * token pair — parsing the trimmed shape keeps the refresh path working even if the server
 * omits login-only fields like `serverSettings`.
 */
@Serializable
internal data class RefreshResponse(val user: RefreshedUser) {
  fun asAbsToken(): AbsToken? = user.accessToken?.let { AbsToken(it, user.refreshToken) }
}

@Serializable
internal data class RefreshedUser(
  val accessToken: String? = null,
  val refreshToken: String? = null,
)

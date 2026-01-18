package app.campfire.network

import app.campfire.account.api.AbsToken
import app.campfire.network.envelopes.LoginResponse
import io.ktor.client.plugins.auth.providers.BearerTokens

fun AbsToken.asBearerTokens(): BearerTokens = BearerTokens(
  accessToken = accessToken,
  refreshToken = refreshToken,
)

fun LoginResponse.asAbsToken(): AbsToken? = user.accessToken?.let { accessToken ->
  AbsToken(accessToken, user.refreshToken)
}

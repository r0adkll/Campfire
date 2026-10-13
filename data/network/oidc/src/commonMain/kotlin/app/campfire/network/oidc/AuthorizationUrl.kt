// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.oidc

import app.campfire.network.ApiException
import app.campfire.network.AuthAudioBookShelfApi
import app.campfire.network.oidc.crypto.Pkce
import io.ktor.http.Url

/**
 * Asks the server for the OpenID authorization URL to open in the browser. The request passes
 * through the same reverse proxy as every other call to the server, so it carries the server's
 * [extraHeaders] too. Failures become [OpenIdException.AuthFailure] with the server's message.
 */
internal suspend fun AuthAudioBookShelfApi.requestAuthorizationUrl(
  serverUrl: String,
  pkce: Pkce,
  state: String,
  extraHeaders: Map<String, String>?,
): Result<Url> = authorization(serverUrl, pkce.codeChallenge, pkce.codeVerifier, state, extraHeaders).fold(
  onSuccess = { response -> Result.success(Url(response.authorizationUrl)) },
  onFailure = { cause ->
    val message = (cause as? ApiException)?.apiMessage?.takeIf { it.isNotBlank() }
      ?: cause.message
      ?: "Unable to fetch authorization URL"
    Result.failure(OpenIdException.AuthFailure(message))
  },
)

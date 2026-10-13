// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.oidc

import android.app.Application
import app.campfire.network.AuthAudioBookShelfApi
import app.campfire.network.oidc.crypto.Pkce
import org.publicvalue.multiplatform.oidc.encodeForPKCE
import org.publicvalue.multiplatform.oidc.secureRandomBytes

class AndroidAuthorizationFlow(
  private val application: Application,
  private val authApi: AuthAudioBookShelfApi,
  private val launcher: StartActivityForResultFlowLauncher,
) : AuthorizationFlow {

  override suspend fun getAuthorization(
    serverUrl: String,
    extraHeaders: Map<String, String>?,
  ): Result<OpenIdAuthorization> {
    val pkce = Pkce()
    val state = secureRandomBytes().encodeForPKCE()

    // 1) Request the /auth/openid request WITHOUT redirects to fetch the OpenID
    //    authorization URL. Additionally capturing any session cookies in the process.
    val requestUrl = authApi.requestAuthorizationUrl(serverUrl, pkce, state, extraHeaders)
      .getOrElse { return Result.failure(it) }

    // 2) Direct user DIRECTLY to the OpenID authorization page instead. Let them finish,
    //    capturing the re-direct to campfireaudiobooks://oauth.
    // 3) Then use that to exchange for access/refresh tokens.
    val webFlow = ActivityWebAuthFlow(
      context = application,
      launcher = launcher,
    )
    val result = webFlow.startWebFlow(
      requestUrl = requestUrl,
      redirectUri = requestUrl.parameters["redirect_uri"].orEmpty(),
    )
    return when (result) {
      is WebAuthFlowResult.Success -> when (val error = getErrorResult<OpenIdAuthorization>(result.responseUrl)) {
        null -> {
          @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
          val code = result.responseUrl!!.parameters["code"]
          val returnedState = result.responseUrl.parameters["state"]

          Result.success(
            OpenIdAuthorization(
              codeVerifier = pkce.codeVerifier,
              code = code.orEmpty(),
              state = returnedState.orEmpty(),
            ),
          )
        }
        else -> error
      }
      WebAuthFlowResult.Cancelled -> Result.failure(OpenIdException.AuthCancelled())
    }
  }
}

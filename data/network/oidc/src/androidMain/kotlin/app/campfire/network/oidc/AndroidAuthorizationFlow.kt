package app.campfire.network.oidc

import android.app.Application
import android.webkit.CookieManager
import app.campfire.core.logging.bark
import app.campfire.network.oidc.crypto.Pkce
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import org.publicvalue.multiplatform.oidc.encodeForPKCE
import org.publicvalue.multiplatform.oidc.secureRandomBytes

class AndroidAuthorizationFlow(
  private val application: Application,
  private val launcher: StartActivityForResultFlowLauncher,
) : AuthorizationFlow {

  override suspend fun getAuthorization(serverUrl: String): Result<OpenIdAuthorization> {
    val webFlow = ActivityWebAuthFlow(
      context = application,
      launcher = launcher,
    )

    val pkce = Pkce()
    val state = secureRandomBytes().encodeForPKCE()
    val requestUrl = URLBuilder(serverUrl).apply {
      appendPathSegments("auth", "openid")
      parameters["code_challenge"] = pkce.codeChallenge
      parameters["code_challenge_method"] = "S256"
      parameters["response_type"] = "code"
      parameters["redirect_uri"] = "audiobookshelf://oauth"
      parameters["client_id"] = "Campfire"
      parameters["state"] = state
    }.build()
    val result = webFlow.startWebFlow(requestUrl, requestUrl.parameters["redirect_uri"].orEmpty())

    return when (result) {
      is WebAuthFlowResult.Success -> when (val error = getErrorResult<OpenIdAuthorization>(result.responseUrl)) {
        null -> {
          val code = result.responseUrl!!.parameters["code"]
          val returnedState = result.responseUrl.parameters["state"]

          // Parse cookies from webview
          val cookie = CookieManager.getInstance()
            .getCookie(requestUrl.toString())

          bark {
            """
              OIDC Result: ${result.responseUrl}
              -- code = $code,
              -- state = $returnedState,
              -- localState = $state,
              -- codeVerifier = ${pkce.codeVerifier},
              -- cookie = $cookie,
            """.trimIndent()
          }

          Result.success(
            OpenIdAuthorization(
              codeVerifier = pkce.codeVerifier,
              code = code.orEmpty(),
              state = returnedState.orEmpty(),
              cookie = cookie,
            ),
          )
        }
        else -> error
      }
      WebAuthFlowResult.Cancelled -> Result.failure(OpenIdException.AuthCancelled())
    }
  }
}

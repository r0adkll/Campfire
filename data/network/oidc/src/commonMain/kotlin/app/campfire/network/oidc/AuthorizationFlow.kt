package app.campfire.network.oidc

import io.ktor.http.Url
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

interface AuthorizationFlow {

  /**
   * Start the authorization process to get the information needed to complete the open id auth
   * flow.
   */
  suspend fun getAuthorization(
    serverUrl: String,
    extraHeaders: Map<String, String>? = null,
  ): Result<OpenIdAuthorization>
}

@OptIn(ExperimentalContracts::class)
internal fun <T> getErrorResult(responseUri: Url?): Result<T>? {
  contract { returns(null) implies (responseUri != null) }
  if (responseUri != null) {
    if (responseUri.parameters.contains("error")) {
      // error
      return Result.failure(
        OpenIdException.AuthFailure(
          message = responseUri.parameters["error"] ?: "",
        ),
      )
    }
  } else {
    return Result.failure(OpenIdException.AuthFailure(message = "No Uri in callback from browser (was $responseUri)."))
  }
  return null
}

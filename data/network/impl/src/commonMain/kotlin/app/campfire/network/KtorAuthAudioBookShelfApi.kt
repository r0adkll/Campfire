package app.campfire.network

import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.network.di.ReturnTokens
import app.campfire.network.di.ServerUrl
import app.campfire.network.envelopes.AuthorizationResponse
import app.campfire.network.envelopes.LoginRequest
import app.campfire.network.envelopes.LoginResponse
import app.campfire.network.models.NetworkModel
import app.campfire.network.models.ServerStatus
import com.r0adkll.kimchi.annotations.ContributesBinding
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSecure
import io.ktor.http.isSuccess
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import me.tatarka.inject.annotations.Inject

@SingleIn(AppScope::class)
@Inject
@ContributesBinding(AppScope::class)
class KtorAuthAudioBookShelfApi(
  private val httpClient: HttpClient,
  private val dispatcherProvider: DispatcherProvider,
) : AuthAudioBookShelfApi {

  // We need the in-memory cookie storage to remain consistent
  // between HttpClients since we copy them to enable/disable
  // automatic redirects.
  private val cookieStorage = AcceptAllCookiesStorage()

  private val client by lazy {
    httpClient.config {
      install(HttpCookies) {
        storage = cookieStorage
      }
    }
  }

  override suspend fun status(
    serverUrl: String,
    extraHeaders: Map<String, String>?,
  ): Result<ServerStatus> {
    return trySendRequest {
      client.get("$serverUrl/status") {
        maybeHeaders(extraHeaders)
      }
    }
  }

  override suspend fun login(
    serverUrl: String,
    username: String,
    password: String,
    extraHeaders: Map<String, String>?,
  ): Result<LoginResponse> = trySendRequest {
    client.post {
      url("${cleanServerUrl(serverUrl)}/login")
      header(HttpHeaders.ReturnTokens, "true")
      maybeHeaders(extraHeaders)
      contentType(ContentType.Application.Json)
      setBody(LoginRequest(username, password))
    }
  }

  override suspend fun authorization(
    serverUrl: String,
    codeChallenge: String,
    codeVerifier: String,
    state: String,
    extraHeaders: Map<String, String>?,
  ): Result<AuthorizationResponse> {
    val nonRedirectClient = client.config {
      followRedirects = false
    }

    return try {
      val response = nonRedirectClient.get {
        url("${cleanServerUrl(serverUrl)}/auth/openid")
        parameter("code_challenge", codeChallenge)
        parameter("code_challenge_method", "S256")
        parameter("response_type", "code")
        parameter("redirect_uri", "audiobookshelf://oauth")
        parameter("client_id", "Campfire")
        parameter("state", state)
        maybeHeaders(extraHeaders)
      }

      if (response.status.value in 200 until 400) {
        val redirectUrl = response.headers[HttpHeaders.Location]
        if (redirectUrl != null) {
          val redirectUrl = Url(redirectUrl)

          if (!redirectUrl.protocol.isSecure()) {
            return Result.failure(ApiException(response.status.value, "Redirect URL is not secure!"))
          }

          Result.success(AuthorizationResponse(redirectUrl.toString()))
        } else {
          Result.failure(ApiException(response.status.value, "No 'Location' header found!"))
        }
      } else {
        Result.failure(ApiException(response.status.value, "/auth/openid failed!"))
      }
    } catch (e: IOException) {
      e.printStackTrace()
      Result.failure(e)
    } catch (e: NoTransformationFoundException) {
      e.printStackTrace()
      Result.failure(e)
    }
  }

  override suspend fun oauth(
    serverUrl: String,
    state: String,
    code: String,
    codeVerifier: String,
    extraHeaders: Map<String, String>?,
  ): Result<LoginResponse> = trySendRequest {
    client.get {
      val baseUrl = cleanServerUrl(serverUrl)
      url("$baseUrl/auth/openid/callback")
      parameter("state", state)
      parameter("code", code)
      parameter("code_verifier", codeVerifier)
      maybeHeaders(extraHeaders)
    }
  }

  private suspend inline fun <reified T> trySendRequest(
    noinline responseMapper: suspend (HttpResponse) -> T = { it.body<T>() },
    crossinline request: suspend () -> HttpResponse,
  ): Result<T> = withContext(dispatcherProvider.io) {
    try {
      val response = request()
      if (response.status.isSuccess()) {
        val originServerUrl = response.call.request.headers[HttpHeaders.ServerUrl]
        val body = responseMapper(response)
        if (body is NetworkModel && originServerUrl != null) {
          body.applyOrigin(RequestOrigin.Url(originServerUrl))
        }

        Result.success(body)
      } else {
        Result.failure(ApiException(response.status.value, response.bodyAsText()))
      }
    } catch (e: IOException) {
      e.printStackTrace()
      Result.failure(e)
    } catch (e: NoTransformationFoundException) {
      e.printStackTrace()
      Result.failure(e)
    } catch (e: IllegalArgumentException) {
      e.printStackTrace()
      Result.failure(e)
    }
  }
}

private fun HttpRequestBuilder.maybeHeaders(headers: Map<String, String>?) {
  headers?.let { h ->
    h.forEach { (k, v) -> header(k, v) }
  }
}

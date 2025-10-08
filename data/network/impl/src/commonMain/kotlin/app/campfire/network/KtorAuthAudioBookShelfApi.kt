package app.campfire.network

import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.network.di.ReturnTokens
import app.campfire.network.di.ServerUrl
import app.campfire.network.envelopes.LoginRequest
import app.campfire.network.envelopes.LoginResponse
import app.campfire.network.envelopes.PingResponse
import app.campfire.network.models.NetworkModel
import app.campfire.network.models.ServerStatus
import com.r0adkll.kimchi.annotations.ContributesBinding
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.cookie
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
import io.ktor.http.isSuccess
import io.ktor.http.parseClientCookiesHeader
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import me.tatarka.inject.annotations.Inject

@Inject
@ContributesBinding(AppScope::class)
class KtorAuthAudioBookShelfApi(
  private val httpClient: HttpClient,
  private val dispatcherProvider: DispatcherProvider,
) : AuthAudioBookShelfApi {

  private val client by lazy {
    httpClient.config {
      install(HttpCookies)
    }
  }

  override suspend fun ping(
    serverUrl: String,
  ): Boolean = trySendRequest<PingResponse> { client.get("$serverUrl/ping") }
    .map { it.success }
    .getOrElse { false }

  override suspend fun status(serverUrl: String): Result<ServerStatus> {
    return trySendRequest { client.get("$serverUrl/status") }
  }

  override suspend fun login(
    serverUrl: String,
    username: String,
    password: String,
  ): Result<LoginResponse> = trySendRequest {
    client.post {
      header(HttpHeaders.ReturnTokens, "true")
      url("${cleanServerUrl(serverUrl)}/login")
      contentType(ContentType.Application.Json)
      setBody(LoginRequest(username, password))
    }
  }

  override suspend fun oauth(
    serverUrl: String,
    state: String,
    code: String,
    codeVerifier: String,
    cookie: String,
  ): Result<LoginResponse> = trySendRequest {
    client.get {
      val baseUrl = cleanServerUrl(serverUrl)
      url("$baseUrl/auth/openid/callback")
      parameter("state", state)
      parameter("code", code)
      parameter("code_verifier", codeVerifier)

      cookie("auth_cb", "$baseUrl/audiobookshelf/login", domain = baseUrl)
      parseClientCookiesHeader(cookie).forEach { (name, value) ->
        cookie(
          name = name,
          value = value,
          domain = baseUrl,
        )
      }
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
    }
  }
}

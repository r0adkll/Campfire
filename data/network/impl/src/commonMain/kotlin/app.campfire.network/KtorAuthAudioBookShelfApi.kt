package app.campfire.network

import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.network.KtorAudioBookShelfApi.Companion.HEADER_SERVER_URL
import app.campfire.network.envelopes.Envelope
import app.campfire.network.envelopes.LoginRequest
import app.campfire.network.envelopes.LoginResponse
import app.campfire.network.envelopes.PingResponse
import app.campfire.network.models.NetworkModel
import com.r0adkll.kimchi.annotations.ContributesBinding
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
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
      defaultRequest {
      }

      install(ContentNegotiation) {
        json()
      }
    }
  }

  override suspend fun ping(
    serverUrl: String,
  ): Boolean = trySendRequest<PingResponse> { client.get(Url("$serverUrl/ping")) }
    .map { it.success }
    .getOrElse { false }

  override suspend fun login(
    serverUrl: String,
    username: String,
    password: String,
  ): Result<LoginResponse> = trySendRequest {
    client.post {
      url("${cleanServerUrl(serverUrl)}/login")
      contentType(ContentType.Application.Json)
      setBody(LoginRequest(username, password))
    }
  }

  private suspend inline fun <reified T> trySendRequest(
    noinline responseMapper: suspend (HttpResponse) -> T = { it.body<T>() },
    crossinline request: suspend () -> HttpResponse,
  ): Result<T> = withContext(dispatcherProvider.io) {
    try {
      val response = request()
      if (response.status.isSuccess()) {
        val originServerUrl = response.call.request.headers[HEADER_SERVER_URL]
        val body = responseMapper(response)
        if (body is NetworkModel && originServerUrl != null) {
          body.origin = RequestOrigin.Url(originServerUrl)
        }

        // If our response model is an [Envelope] be sure to apply
        // its postage.
        if (body is Envelope) {
          body.applyPostage()
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

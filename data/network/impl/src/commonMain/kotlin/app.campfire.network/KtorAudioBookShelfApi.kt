package app.campfire.network

import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.logging.bark
import app.campfire.network.envelopes.LoginRequest
import app.campfire.network.envelopes.LoginResponse
import app.campfire.network.envelopes.PingResponse
import com.r0adkll.kotlininject.merge.annotations.ContributesBinding
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
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
class KtorAudioBookShelfApi(
  private val httpClient: HttpClient,
  private val dispatcherProvider: DispatcherProvider,
) : AudioBookShelfApi {

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
  ): Boolean = withContext(dispatcherProvider.io){
    try {
      val response = client.get(Url("$serverUrl/ping"))
      if (response.status.isSuccess()) {
        response.body<PingResponse>().success
      } else {
        false
      }
    } catch (e: IOException) {
      bark(throwable = e) { "Error pinging server: $serverUrl" }
      false
    }
  }

  override suspend fun login(
    serverUrl: String,
    username: String,
    password: String,
  ): Result<LoginResponse> = withContext(dispatcherProvider.io) {
    try {
      val response = client.post {
        url("${cleanServerUrl(serverUrl)}/login")
        contentType(ContentType.Application.Json)
        setBody(LoginRequest(username, password))
      }

      if (response.status.isSuccess()) {
        val body = response.body<LoginResponse>()
        Result.success(body)
      } else {
        Result.failure(ApiException(response.status.value, response.bodyAsText()))
      }
    } catch (e: IOException) {
      Result.failure(e)
    }
  }

  override suspend fun authorize(): Result<LoginResponse> = withContext(dispatcherProvider.io) {
    try {
      val response = client.post {
        url("${cleanServerUrl("serverUrl")}/authorize")
      }

      if (response.status.isSuccess()) {
        val body = response.body<LoginResponse>()
        Result.success(body)
      } else {
        Result.failure(ApiException(response.status.value, response.bodyAsText()))
      }
    } catch (e: IOException) {
      Result.failure(e)
    }
  }

  private fun cleanServerUrl(url: String): String {
    fun String.withoutFinalSlash(): String = if (last() == '/') {
      substringBeforeLast('/')
    } else {
      this
    }

    return if (url.startsWith("http://") || url.startsWith("https://")) {
      url.withoutFinalSlash()
    } else {
      "https://${url.withoutFinalSlash()}"
    }
  }
}

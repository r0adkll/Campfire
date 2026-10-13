// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network

import app.campfire.core.coroutines.DispatcherProvider
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.prop
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

/**
 * A server behind Cloudflare Access answers an unauthenticated `/status` with Access's own login
 * challenge; the probe reports that instead of failing to parse the login page.
 */
class ServerStatusAccessTest {

  private val statusJson = """{"app":"audiobookshelf","serverVersion":"2.30.0","isInit":true,"language":"en-us"}"""
  private val requests = mutableListOf<String>()

  private fun TestScope.api(handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
    KtorAuthAudioBookShelfApi(
      httpClient = HttpClient(
        MockEngine { request ->
          requests += request.url.toString()
          handler(request)
        },
      ) {
        install(ContentNegotiation) {
          json(Json { ignoreUnknownKeys = true })
        }
      },
      dispatcherProvider = UnconfinedTestDispatcher(testScheduler).let { dispatcher ->
        DispatcherProvider(dispatcher, dispatcher, dispatcher, dispatcher, dispatcher)
      },
    )

  private fun MockRequestHandleScope.status() =
    respond(statusJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

  @Test
  fun accessLoginRedirectIsReportedAsCloudflareAccess() = runTest {
    val api = api {
      respond(
        content = "",
        status = HttpStatusCode.Found,
        headers = headersOf(
          HttpHeaders.Location,
          "https://MyTeam.cloudflareaccess.com/cdn-cgi/access/login/abs.example.com?redirect_url=%2Fstatus",
        ),
      )
    }

    val error = api.status("https://abs.example.com").exceptionOrNull()

    assertThat(error).isNotNull().isInstanceOf<CloudflareAccessException>()
      .prop(CloudflareAccessException::teamDomain).isEqualTo("myteam.cloudflareaccess.com")
    assertThat(requests).containsExactly("https://abs.example.com/status")
  }

  @Test
  fun accessDenialPageIsReportedAsCloudflareAccess() = runTest {
    val api = api {
      respond(
        content = "<html>Forbidden</html>",
        status = HttpStatusCode.Forbidden,
        headers = headersOf(
          HttpHeaders.Server to listOf("cloudflare"),
          HttpHeaders.ContentType to listOf("text/html; charset=UTF-8"),
        ),
      )
    }

    val error = api.status("https://abs.example.com").exceptionOrNull()

    assertThat(error).isNotNull().isInstanceOf<CloudflareAccessException>()
      .prop(CloudflareAccessException::teamDomain).isNull()
  }

  @Test
  fun otherRedirectsAreStillFollowed() = runTest {
    val api = api { request ->
      when (request.url.toString()) {
        "http://abs.example.com/status" -> respond(
          content = "",
          status = HttpStatusCode.MovedPermanently,
          headers = headersOf(HttpHeaders.Location, "https://abs.example.com/status"),
        )
        else -> status()
      }
    }

    val result = api.status("http://abs.example.com")

    assertThat(result.getOrNull()?.serverVersion).isEqualTo("2.30.0")
  }

  @Test
  fun serviceTokenHeadersReachTheServer() = runTest {
    var clientId: String? = null
    val api = api { request ->
      clientId = request.headers["CF-Access-Client-Id"]
      status()
    }

    val result = api.status("https://abs.example.com", mapOf("CF-Access-Client-Id" to "id.access"))

    assertThat(result.isSuccess).isEqualTo(true)
    assertThat(clientId).isEqualTo("id.access")
  }

  @Test
  fun forbiddenFromTheServerItselfIsNotAccess() = runTest {
    val api = api {
      respond("""{"error":"nope"}""", HttpStatusCode.Forbidden, headersOf(HttpHeaders.ContentType, "application/json"))
    }

    val error = api.status("https://abs.example.com").exceptionOrNull()

    assertThat(error).isNotNull().isInstanceOf<ApiException>()
  }
}

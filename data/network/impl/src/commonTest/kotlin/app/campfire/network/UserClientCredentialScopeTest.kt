// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network

import app.campfire.account.api.AbsToken
import app.campfire.core.session.UserSession
import app.campfire.network.di.installUserAuth
import app.campfire.network.di.installUserExtraHeaders
import app.campfire.network.di.isOnServer
import app.campfire.network.di.userBearerAuthProvider
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

/**
 * The user client also loads images from other hosts (book info provider covers, podcast feed
 * artwork), so the user's bearer token and extra headers must only go to their own server.
 */
class UserClientCredentialScopeTest {

  private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

  private val accountManager = FakeAccountManager().apply {
    tokens[testUser.id] = AbsToken("access", "refresh")
    extraHeaders = mapOf("X-Proxy-Auth" to "secret")
  }
  private val sessionManager = FakeUserSessionManager(UserSession.LoggedIn(testUser))

  private data class SentRequest(val url: String, val authorization: String?, val proxyAuth: String?)

  private val sent = mutableListOf<SentRequest>()

  private fun client(engine: MockEngine) = HttpClient(engine) {
    install(ContentNegotiation) {
      json(Json { ignoreUnknownKeys = true })
    }
    installUserAuth(userBearerAuthProvider(sessionManager, accountManager), sessionManager)
    installUserExtraHeaders(sessionManager, accountManager)
  }

  private fun recordingEngine(status: HttpStatusCode = HttpStatusCode.OK) = MockEngine { request ->
    sent += SentRequest(
      url = request.url.toString(),
      authorization = request.headers[HttpHeaders.Authorization],
      proxyAuth = request.headers["X-Proxy-Auth"],
    )
    when (request.url.encodedPath) {
      "/auth/refresh" -> error("No refresh expected: ${request.url}")
      else -> respond(
        content = "{}",
        status = status,
        headers = headersOf(
          HttpHeaders.ContentType to listOf("application/json"),
          HttpHeaders.WWWAuthenticate to listOf("Bearer"),
        ),
      )
    }
  }

  @Test
  fun serverRequestCarriesCredentials() = runTest {
    client(recordingEngine()).get("https://abs.example.com/api/me")

    assertThat(sent).containsExactly(
      SentRequest("https://abs.example.com/api/me", "Bearer access", "secret"),
    )
  }

  @Test
  fun thirdPartyImageCarriesNoCredentials() = runTest {
    client(recordingEngine()).get("https://covers.openlibrary.org/b/id/12345-L.jpg")

    assertThat(sent).containsExactly(
      SentRequest("https://covers.openlibrary.org/b/id/12345-L.jpg", null, null),
    )
  }

  @Test
  fun thirdPartyUnauthorizedIsNotRetriedWithTheToken() = runTest {
    val response = client(recordingEngine(HttpStatusCode.Unauthorized))
      .get("https://feeds.example.org/artwork.jpg")

    assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
    assertThat(sent).containsExactly(
      SentRequest("https://feeds.example.org/artwork.jpg", null, null),
    )
  }

  @Test
  fun sameHostOnAnotherPortIsAnotherServer() = runTest {
    client(recordingEngine()).get("https://abs.example.com:8443/api/me")

    assertThat(sent).containsExactly(
      SentRequest("https://abs.example.com:8443/api/me", null, null),
    )
  }

  @Test
  fun serverMatchingComparesHostAndPort() {
    assertThat(Url("https://ABS.example.com/api/me").isOnServer("https://abs.example.com/")).isTrue()
    assertThat(Url("wss://abs.example.com/socket.io/").isOnServer("https://abs.example.com")).isTrue()
    assertThat(Url("https://abs.example.com/api/me").isOnServer("abs.example.com")).isTrue()
    assertThat(Url("http://192.168.1.5:13378/api/me").isOnServer("http://192.168.1.5:13378")).isTrue()
    assertThat(Url("http://192.168.1.5:80/api/me").isOnServer("http://192.168.1.5:13378")).isFalse()
    assertThat(Url("https://abs.example.com.evil.net/").isOnServer("https://abs.example.com")).isFalse()
    assertThat(Url("http://abs.example.com/api/me").isOnServer("https://abs.example.com")).isFalse()
  }
}

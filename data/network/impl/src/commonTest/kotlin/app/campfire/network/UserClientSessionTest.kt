// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network

import app.campfire.account.api.AbsToken
import app.campfire.account.api.AccountManager
import app.campfire.account.api.UserSessionManager
import app.campfire.core.session.UserSession
import app.campfire.network.di.installUserAuth
import app.campfire.network.di.installUserExtraHeaders
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

/**
 * Requests on the user client can outlive the session that issued them, so the user-scoped
 * plugins must tolerate a missing or changing session instead of failing the request pipeline.
 */
class UserClientSessionTest {

  private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

  private fun client(
    engine: MockEngine,
    accountManager: AccountManager,
    sessionManager: UserSessionManager,
  ) = HttpClient(engine) {
    install(ContentNegotiation) {
      json(Json { ignoreUnknownKeys = true })
    }
    installUserAuth(sessionManager, accountManager)
    installUserExtraHeaders(sessionManager, accountManager)
  }

  @Test
  fun extraHeadersAreAppliedForLoggedInUser() = runTest {
    val accountManager = FakeAccountManager().apply {
      tokens[testUser.id] = AbsToken("access", "refresh")
      extraHeaders = mapOf("X-Proxy-Auth" to "secret")
    }
    val sessionManager = FakeUserSessionManager(UserSession.LoggedIn(testUser))
    val proxyHeaders = mutableListOf<String?>()
    val engine = MockEngine { request ->
      proxyHeaders += request.headers["X-Proxy-Auth"]
      respond("{}", HttpStatusCode.OK, jsonHeaders)
    }

    client(engine, accountManager, sessionManager).get("https://abs.example.com/api/me")

    assertThat(proxyHeaders).containsExactly("secret")
  }

  @Test
  fun requestWithoutUserIsSentWithoutExtraHeaders() = runTest {
    val accountManager = FakeAccountManager().apply {
      extraHeaders = mapOf("X-Proxy-Auth" to "secret")
    }
    val sessionManager = FakeUserSessionManager(UserSession.LoggedOut)
    val proxyHeaders = mutableListOf<String?>()
    val engine = MockEngine { request ->
      proxyHeaders += request.headers["X-Proxy-Auth"]
      respond("{}", HttpStatusCode.OK, jsonHeaders)
    }

    val response = client(engine, accountManager, sessionManager).get("https://abs.example.com/api/me")

    assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    assertThat(proxyHeaders).containsExactly(null)
  }

  @Test
  fun unauthorizedResponseWithoutUserSkipsRefresh() = runTest {
    val accountManager = FakeAccountManager()
    val sessionManager = FakeUserSessionManager(UserSession.LoggedIn(testUser))
    val engine = MockEngine { request ->
      when (request.url.encodedPath) {
        "/api/me" -> {
          // The session ends while the request is in flight.
          sessionManager.current = UserSession.LoggedOut
          respond("""{"error":"Unauthorized"}""", HttpStatusCode.Unauthorized, jsonHeaders)
        }
        else -> error("No refresh expected without a logged-in user: ${request.url}")
      }
    }

    val response = client(engine, accountManager, sessionManager).get("https://abs.example.com/api/me")

    assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
    assertThat(accountManager.invalidatedUser).isNull()
  }

  @Test
  fun refreshedTokenIsStoredForTheUserThatRequestedIt() = runTest {
    val otherUser = testUser.copy(id = "user-2")
    val accountManager = FakeAccountManager().apply {
      tokens[testUser.id] = AbsToken("access-old", "refresh-old")
    }
    val sessionManager = FakeUserSessionManager(UserSession.LoggedIn(testUser))
    val engine = MockEngine { request ->
      when (request.url.encodedPath) {
        "/api/me" -> if (request.headers[HttpHeaders.Authorization] == "Bearer access-new") {
          respond("{}", HttpStatusCode.OK, jsonHeaders)
        } else {
          respond("""{"error":"Unauthorized"}""", HttpStatusCode.Unauthorized, jsonHeaders)
        }
        "/auth/refresh" -> {
          // The user switches accounts while the refresh is in flight.
          sessionManager.current = UserSession.LoggedIn(otherUser)
          respond(
            """{"user":{"accessToken":"access-new","refreshToken":"refresh-new"}}""",
            HttpStatusCode.OK,
            jsonHeaders,
          )
        }
        else -> error("Unexpected request: ${request.url}")
      }
    }

    client(engine, accountManager, sessionManager).get("https://abs.example.com/api/me")

    assertThat(accountManager.tokens[testUser.id]).isEqualTo(AbsToken("access-new", "refresh-new"))
    assertThat(accountManager.tokens[otherUser.id]).isNull()
  }
}

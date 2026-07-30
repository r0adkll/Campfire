// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network

import app.campfire.account.api.AbsToken
import app.campfire.account.api.AccountManager
import app.campfire.account.api.UserSessionManager
import app.campfire.core.model.Server
import app.campfire.core.model.User
import app.campfire.core.model.UserId
import app.campfire.core.session.UserSession
import app.campfire.network.di.installUserAuth
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

/**
 * Exercises [UserClientTokenRefresher] against a client configured with the REAL
 * [installUserAuth] bearer setup, proving the dummy authorize request drives Ktor's own
 * refresh machinery — the single refresh authority — rather than refreshing out-of-band.
 */
class UserClientTokenRefresherTest {

  private val user = User(
    id = "user-1",
    name = "Testy McTestface",
    selectedLibraryId = "lib-1",
    type = User.Type.User,
    isActive = true,
    isLocked = false,
    lastSeen = 0L,
    createdAt = 0L,
    permissions = User.Permissions(
      download = true,
      update = true,
      delete = true,
      upload = true,
      accessAllLibraries = true,
      accessAllTags = true,
      accessExplicitContent = true,
    ),
    serverUrl = "https://abs.example.com",
  )

  /** A fake ABS server that only accepts [validAccessToken] and rotates [validRefreshToken]. */
  private fun MockRequestHandleScope.absServer(
    request: HttpRequestData,
    validAccessToken: String = "access-new",
    validRefreshToken: String = "refresh-old",
  ): HttpResponseData = when (request.url.encodedPath) {
    "/api/authorize" -> {
      if (request.headers[HttpHeaders.Authorization] == "Bearer $validAccessToken") {
        respond("{}", HttpStatusCode.OK, jsonHeaders)
      } else {
        respond("""{"error":"Unauthorized"}""", HttpStatusCode.Unauthorized, jsonHeaders)
      }
    }
    "/auth/refresh" -> {
      if (request.headers["x-refresh-token"] == validRefreshToken) {
        respond(
          """{"user":{"accessToken":"access-new","refreshToken":"refresh-new"}}""",
          HttpStatusCode.OK,
          jsonHeaders,
        )
      } else {
        respond("""{"error":"Invalid refresh token"}""", HttpStatusCode.Unauthorized, jsonHeaders)
      }
    }
    else -> error("Unexpected request: ${request.url}")
  }

  private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

  private fun refresher(
    engine: MockEngine,
    accountManager: AccountManager,
    sessionManager: UserSessionManager,
  ): UserClientTokenRefresher {
    val client = HttpClient(engine) {
      install(ContentNegotiation) {
        json(
          Json {
            ignoreUnknownKeys = true
            isLenient = true
          },
        )
      }
      installUserAuth(sessionManager, accountManager)
    }
    return UserClientTokenRefresher(client, accountManager, sessionManager)
  }

  @Test
  fun staleTokenIsRefreshedThroughTheAuthPlugin() = runTest {
    val accountManager = FakeAccountManager().apply {
      tokens[user.id] = AbsToken("access-old", "refresh-old")
    }
    val sessionManager = FakeUserSessionManager(UserSession.LoggedIn(user))
    val engine = MockEngine { request -> absServer(request) }

    val result = refresher(engine, accountManager, sessionManager)
      .refresh(user.id, user.serverUrl, "access-old")

    assertThat(result).isEqualTo(AbsToken("access-new", "refresh-new"))
    assertThat(accountManager.tokens[user.id]).isEqualTo(AbsToken("access-new", "refresh-new"))
    assertThat(accountManager.invalidatedUser).isNull()
  }

  @Test
  fun deadRefreshTokenInvalidatesTheAccount() = runTest {
    val accountManager = FakeAccountManager().apply {
      tokens[user.id] = AbsToken("access-old", "refresh-dead")
    }
    val sessionManager = FakeUserSessionManager(UserSession.LoggedIn(user))
    val engine = MockEngine { request -> absServer(request) }

    val result = refresher(engine, accountManager, sessionManager)
      .refresh(user.id, user.serverUrl, "access-old")

    assertThat(result).isNull()
    assertThat(accountManager.invalidatedUser).isEqualTo(user)
  }

  @Test
  fun skipsNetworkWhenTokenAlreadyRotatedByHttpLayer() = runTest {
    val accountManager = FakeAccountManager().apply {
      tokens[user.id] = AbsToken("access-already-fresh", "refresh-already-fresh")
    }
    val sessionManager = FakeUserSessionManager(UserSession.LoggedIn(user))
    val engine = MockEngine {
      error("No network call expected when the stored token is already rotated")
    }

    val result = refresher(engine, accountManager, sessionManager)
      .refresh(user.id, user.serverUrl, "access-stale")

    assertThat(result).isEqualTo(AbsToken("access-already-fresh", "refresh-already-fresh"))
  }

  @Test
  fun declinesRefreshForNonCurrentUser() = runTest {
    val accountManager = FakeAccountManager().apply {
      tokens["someone-else"] = AbsToken("access-old", "refresh-old")
    }
    val sessionManager = FakeUserSessionManager(UserSession.LoggedIn(user))
    val engine = MockEngine {
      error("No network call expected for a non-current user")
    }

    val result = refresher(engine, accountManager, sessionManager)
      .refresh("someone-else", user.serverUrl, "access-old")

    assertThat(result).isNull()
  }

  @Test
  fun alreadyFreshTokenSucceedsWithoutRotation() = runTest {
    val accountManager = FakeAccountManager().apply {
      tokens[user.id] = AbsToken("access-new", "refresh-old")
    }
    val sessionManager = FakeUserSessionManager(UserSession.LoggedIn(user))
    val engine = MockEngine { request -> absServer(request) }

    val result = refresher(engine, accountManager, sessionManager)
      .refresh(user.id, user.serverUrl, "access-new")

    assertThat(result).isEqualTo(AbsToken("access-new", "refresh-old"))
    assertThat(accountManager.invalidatedUser).isNull()
  }
}

private class FakeAccountManager : AccountManager {
  val tokens = mutableMapOf<UserId, AbsToken>()
  var extraHeaders: Map<String, String>? = null
  var invalidatedUser: User? = null

  override suspend fun addAccount(
    serverUrl: String,
    accessToken: String,
    refreshToken: String?,
    extraHeaders: Map<String, String>?,
    user: User,
  ) {
    tokens[user.id] = AbsToken(accessToken, refreshToken)
  }

  override suspend fun invalidateAccount(user: User) {
    invalidatedUser = user
    tokens.remove(user.id)
  }

  override suspend fun switchAccount(user: User) = Unit

  override suspend fun logout(server: Server) = Unit

  override suspend fun getToken(userId: UserId): AbsToken? = tokens[userId]

  override suspend fun updateToken(userId: UserId, newToken: AbsToken) {
    tokens[userId] = newToken
  }

  override suspend fun getExtraHeaders(userId: UserId): Map<String, String>? = extraHeaders
}

private class FakeUserSessionManager(initial: UserSession) : UserSessionManager {
  private val state = MutableStateFlow(initial)

  override var current: UserSession
    get() = state.value
    set(value) {
      state.value = value
    }

  override fun observe(): StateFlow<UserSession> = state
}

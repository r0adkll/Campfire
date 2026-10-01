// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.di

import app.campfire.account.api.AbsToken
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.app.Flavor
import app.campfire.core.session.UserSession
import app.campfire.network.FakeAccountManager
import app.campfire.network.FakeUserSessionManager
import app.campfire.network.testUser
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.pluginOrNull
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

/**
 * The [AudioPlayerClient] shares the [UserClient]'s bearer provider, so a refresh through either
 * client serves both and the single-use refresh token is only ever spent once.
 */
class AudioPlayerHttpClientTest {

  private val applicationInfo = ApplicationInfo(
    packageName = "app.campfire",
    debugBuild = true,
    flavor = Flavor.Standard,
    versionName = "1.0",
    versionCode = 1,
    osName = "test",
    osVersion = "1",
  )

  private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

  private val accountManager = FakeAccountManager().apply {
    tokens[testUser.id] = AbsToken("access-old", "refresh-old")
  }
  private val sessionManager = FakeUserSessionManager(UserSession.LoggedIn(testUser))
  private val bearerAuthProvider = userBearerAuthProvider(sessionManager, accountManager)

  private var refreshCount = 0

  /** A fake ABS server that only accepts `access-new`, which a refresh with `refresh-old` issues. */
  private fun MockRequestHandleScope.absServer(request: HttpRequestData): HttpResponseData =
    when (request.url.encodedPath) {
      "/auth/refresh" -> {
        refreshCount++
        if (request.headers["x-refresh-token"] == "refresh-old") {
          respond(
            """{"user":{"accessToken":"access-new","refreshToken":"refresh-new"}}""",
            HttpStatusCode.OK,
            jsonHeaders,
          )
        } else {
          respond("""{"error":"Invalid refresh token"}""", HttpStatusCode.Unauthorized, jsonHeaders)
        }
      }
      else -> if (request.headers[HttpHeaders.Authorization] == "Bearer access-new") {
        respond("audio-bytes", HttpStatusCode.OK)
      } else {
        respond("""{"error":"Unauthorized"}""", HttpStatusCode.Unauthorized, jsonHeaders)
      }
    }

  private fun audioPlayerClient(engine: MockEngine) = HttpClient(engine) {
    configureAudioPlayerClient(applicationInfo, sessionManager, accountManager, bearerAuthProvider)
  }

  private fun userClient(engine: MockEngine) = HttpClient(engine) {
    install(ContentNegotiation) {
      json(Json { ignoreUnknownKeys = true })
    }
    installUserAuth(bearerAuthProvider)
  }

  @Test
  fun streamsWithoutAResponseCacheButWithTheSharedAuth() {
    val client =
      HttpClient { configureAudioPlayerClient(applicationInfo, sessionManager, accountManager, bearerAuthProvider) }

    assertThat(client.pluginOrNull(HttpCache)).isNull()
    assertThat(client.pluginOrNull(HttpTimeout)).isNotNull()
    assertThat(client.pluginOrNull(Auth)).isNotNull()
  }

  @Test
  fun refreshFromAudioPlayerClientIsUsedByUserClient() = runTest {
    val userClientAuthHeaders = mutableListOf<String?>()
    val audioClient = audioPlayerClient(MockEngine { absServer(it) })
    val userClient = userClient(
      MockEngine { request ->
        userClientAuthHeaders += request.headers[HttpHeaders.Authorization]
        absServer(request)
      },
    )

    val audioResponse = audioClient.get("https://abs.example.com/api/items/li-1/file/1")
    val userResponse = userClient.get("https://abs.example.com/api/me")

    assertThat(audioResponse.status).isEqualTo(HttpStatusCode.OK)
    assertThat(userResponse.status).isEqualTo(HttpStatusCode.OK)
    assertThat(userClientAuthHeaders).containsExactly("Bearer access-new")
    assertThat(refreshCount).isEqualTo(1)
    assertThat(accountManager.tokens[testUser.id]).isEqualTo(AbsToken("access-new", "refresh-new"))
  }

  @Test
  fun unauthorizedForTokenAnotherClientRotatedRetriesWithoutRefreshingAgain() = runTest {
    val userClient = userClient(MockEngine { absServer(it) })
    var refreshedMidRequest = false
    val audioClient = audioPlayerClient(
      MockEngine { request ->
        if (!refreshedMidRequest) {
          // The user client refreshes while this request, sent with the old token, is in flight.
          refreshedMidRequest = true
          userClient.get("https://abs.example.com/api/me")
        }
        absServer(request)
      },
    )

    val response = audioClient.get("https://abs.example.com/api/items/li-1/file/1")

    assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    assertThat(refreshCount).isEqualTo(1)
  }

  @Test
  fun providerIsASingleInstanceSharedByBothClients() {
    val audioClient = audioPlayerClient(MockEngine { absServer(it) })
    val userClient = userClient(MockEngine { absServer(it) })

    assertThat(audioClient.authProvider<BearerAuthProvider>()).isNotNull()
    assertThat(audioClient.authProvider<BearerAuthProvider>())
      .isEqualTo(userClient.authProvider<BearerAuthProvider>())
  }
}

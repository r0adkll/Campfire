// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.di

import app.campfire.account.api.AccountManager
import app.campfire.account.api.UserSessionManager
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.session.user
import app.campfire.core.session.userId
import app.campfire.network.RefreshResponse
import app.campfire.network.asBearerTokens
import app.campfire.network.cleanServerUrl
import app.campfire.network.plugins.suspendingDefaultHeaders
import app.campfire.network.reachability.ReachabilityGate
import app.campfire.network.reachability.serverReachabilityPlugin
import com.livewire.plugin.network.ktor.LivewireNetworkPlugin
import com.r0adkll.kimchi.annotations.ContributesTo
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodedPath
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Provides

private val RESPONSE_CODE_REGEX = "RESPONSE: (\\d+)".toRegex(RegexOption.MULTILINE)

internal val HttpHeaders.RefreshToken: String get() = "x-refresh-token"
internal val HttpHeaders.ReturnTokens: String get() = "x-return-tokens"
internal val HttpHeaders.ServerUrl: String get() = "X-Server-Url"

@ContributesTo(AppScope::class)
interface HttpClientModule {

  @BaseClient
  @SingleIn(AppScope::class)
  @Provides
  fun provideHttpClient(
    applicationInfo: ApplicationInfo,
  ): HttpClient {
    return HttpClient {
      // Debug-only network inspection. This installs a ResponseObserver that reads the full
      // body of every response, so it must never ride along on release builds.
      if (applicationInfo.debugBuild) {
        install(LivewireNetworkPlugin)
      }

      install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = 30_000
        socketTimeoutMillis = 30_000
      }

      install(ContentNegotiation) {
        json(
          Json {
            isLenient = true
            ignoreUnknownKeys = true
          },
        )
      }

      install(HttpCache)

      install(Logging) {
        // Ktor logs the full request URL (user's private server host included) at INFO,
        // so this must stay NONE on anything that forwards logs to crash reporting.
        level = when {
          applicationInfo.debugBuild -> LogLevel.INFO
          else -> LogLevel.NONE
        }

        filter { builder ->
          // Ignore image requests
          val path = builder.url.encodedPath
          !path.endsWith("/cover") && !path.endsWith("/image")
        }

        logger = object : Logger {
          override fun log(message: String) {
            val responseCode = RESPONSE_CODE_REGEX.find(message)
              ?.groupValues?.getOrNull(1)?.toIntOrNull()
              ?: -1

            val priority = when (responseCode) {
              in 200 until 300 -> LogPriority.INFO
              in 300 until 400 -> LogPriority.WARN
              in 400 until 600 -> LogPriority.ERROR
              else -> LogPriority.DEBUG
            }

            bark(
              tag = "KtorClient",
              priority = priority,
            ) { message }
          }
        }

        sanitizeHeader { header ->
          if (applicationInfo.debugBuild) return@sanitizeHeader false
          header == HttpHeaders.Authorization ||
            header == HttpHeaders.Cookie ||
            header == HttpHeaders.SetCookie ||
            header == HttpHeaders.RefreshToken ||
            header == HttpHeaders.ServerUrl
        }
      }

      defaultRequest {
        header(
          HttpHeaders.UserAgent,
          applicationInfo.userAgent,
        )
      }
    }
  }

  @DownloadClient
  @SingleIn(AppScope::class)
  @Provides
  fun provideDownloadHttpClient(
    applicationInfo: ApplicationInfo,
  ): HttpClient = createDownloadHttpClient(applicationInfo)

  @UserClient
  @SingleIn(AppScope::class)
  @Provides
  fun provideHttpClient(
    @BaseClient baseClient: HttpClient,
    userSessionManager: UserSessionManager,
    accountManager: AccountManager,
    applicationInfo: ApplicationInfo,
    reachabilityGate: ReachabilityGate,
  ): HttpClient {
    return baseClient.config {
      install(serverReachabilityPlugin(reachabilityGate))

      install(WebSockets) {
        pingIntervalMillis = 20_000
      }

      installUserAuth(userSessionManager, accountManager)
      installUserExtraHeaders(userSessionManager, accountManager)
    }
  }
}

/**
 * Builds the [DownloadClient]: deliberately not derived from the base client, whose `HttpCache`
 * (and, in debug builds, Livewire inspection) reads whole responses into memory. There's no
 * request timeout, since a download legitimately takes minutes; the socket timeout instead
 * abandons a transfer that stops receiving data.
 */
internal fun createDownloadHttpClient(applicationInfo: ApplicationInfo): HttpClient = HttpClient {
  install(HttpTimeout) {
    connectTimeoutMillis = 15_000
    socketTimeoutMillis = 60_000
  }

  defaultRequest {
    header(HttpHeaders.UserAgent, applicationInfo.userAgent)
  }
}

/**
 * Applies the current session user's extra headers (e.g. reverse proxy auth) to every request.
 * Requests can outlive the session that issued them (logout, account switch), so a request made
 * without a logged-in user goes out without extra headers rather than failing the pipeline.
 */
internal fun HttpClientConfig<*>.installUserExtraHeaders(
  userSessionManager: UserSessionManager,
  accountManager: AccountManager,
) {
  suspendingDefaultHeaders {
    val userId = userSessionManager.current.userId ?: return@suspendingDefaultHeaders
    accountManager.getExtraHeaders(userId)?.forEach { (name, value) ->
      header(name, value)
    }
  }
}

/**
 * Installs bearer auth for the current session's user. This is the ONLY place tokens are
 * refreshed — Ktor's Auth plugin caches the bearer pair internally and single-flights its own
 * refresh, so refreshing anywhere else would rotate the single-use refresh token underneath
 * the plugin's cached copy and orphan it. Anything outside the HTTP client that needs a fresh
 * token (e.g. the socket) must trigger this plugin via an authenticated request instead — see
 * `UserClientTokenRefresher`.
 */
internal fun HttpClientConfig<*>.installUserAuth(
  userSessionManager: UserSessionManager,
  accountManager: AccountManager,
) {
  install(Auth) {
    bearer {
      loadTokens {
        userSessionManager.current.userId?.let { userId ->
          accountManager.getToken(userId)?.asBearerTokens()
        }
      }

      refreshTokens {
        // Pin the user for the whole refresh: the session can end or switch while the refresh
        // request is in flight, and the rotated token belongs to the user it was issued for.
        val user = userSessionManager.current.user ?: return@refreshTokens null
        val tokens = accountManager.getToken(user.id)
        val newTokenResponse = client.post {
          url("${cleanServerUrl(user.serverUrl)}/auth/refresh")
          tokens?.refreshToken?.let {
            header(HttpHeaders.RefreshToken, it)
          }
          markAsRefreshTokenRequest()
        }

        return@refreshTokens if (newTokenResponse.status.isSuccess()) {
          try {
            val newToken = newTokenResponse.body<RefreshResponse>().asAbsToken()
            if (newToken != null) {
              accountManager.updateToken(user.id, newToken)
              newToken.asBearerTokens()
            } else {
              bark("KtorClient", LogPriority.ERROR) { "No valid tokens in response, requiring authentication…" }
              accountManager.invalidateAccount(user)
              null
            }
          } catch (e: Exception) {
            bark("KtorClient", LogPriority.ERROR) { "Something went wrong trying to parse refresh token response" }
            null
          }
        } else {
          bark("KtorClient", LogPriority.ERROR) { "[${newTokenResponse.status}] Refresh token request failed!" }
          if (
            newTokenResponse.status == HttpStatusCode.Unauthorized ||
            newTokenResponse.status == HttpStatusCode.Forbidden
          ) {
            accountManager.invalidateAccount(user)
          }
          null
        }
      }
    }
  }
}

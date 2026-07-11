package app.campfire.network.di

import app.campfire.account.api.AccountManager
import app.campfire.account.api.UserSessionManager
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.app.Flavor
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.session.requireServerUrl
import app.campfire.core.session.requiredUser
import app.campfire.core.session.requiredUserId
import app.campfire.core.session.userId
import app.campfire.network.RefreshResponse
import app.campfire.network.asBearerTokens
import app.campfire.network.cleanServerUrl
import app.campfire.network.plugins.suspendingDefaultHeaders
import com.livewire.plugin.network.ktor.LivewireNetworkPlugin
import com.r0adkll.kimchi.annotations.ContributesTo
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
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
      install(LivewireNetworkPlugin)

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
        level = when {
          applicationInfo.debugBuild -> LogLevel.INFO
          applicationInfo.flavor == Flavor.Alpha -> LogLevel.INFO
          else -> LogLevel.NONE
        }

        filter { builder ->
          // Ignore image requests
          !builder.url.toString().endsWith("/cover")
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

  @UserClient
  @SingleIn(AppScope::class)
  @Provides
  fun provideHttpClient(
    @BaseClient baseClient: HttpClient,
    userSessionManager: UserSessionManager,
    accountManager: AccountManager,
    applicationInfo: ApplicationInfo,
  ): HttpClient {
    return baseClient.config {
      install(WebSockets) {
        pingIntervalMillis = 20_000
      }

      installUserAuth(userSessionManager, accountManager)

      suspendingDefaultHeaders {
        val extraHeaders = accountManager.getExtraHeaders(userSessionManager.current.requiredUserId)
        extraHeaders?.forEach { (name, value) ->
          header(name, value)
        }
      }
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
        val tokens = accountManager.getToken(userSessionManager.current.requiredUserId)
        val newTokenResponse = client.post {
          val currentServerUrl = userSessionManager.current.requireServerUrl
          url("${cleanServerUrl(currentServerUrl)}/auth/refresh")
          tokens?.refreshToken?.let {
            header(HttpHeaders.RefreshToken, it)
          }
          markAsRefreshTokenRequest()
        }

        return@refreshTokens if (newTokenResponse.status.isSuccess()) {
          try {
            val newToken = newTokenResponse.body<RefreshResponse>().asAbsToken()
            if (newToken != null) {
              accountManager.updateToken(userSessionManager.current.requiredUserId, newToken)
              newToken.asBearerTokens()
            } else {
              bark("KtorClient", LogPriority.ERROR) { "No valid tokens in response, requiring authentication…" }
              accountManager.invalidateAccount(userSessionManager.current.requiredUser)
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
            accountManager.invalidateAccount(userSessionManager.current.requiredUser)
          }
          null
        }
      }
    }
  }
}

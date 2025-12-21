package app.campfire.network.di

import app.campfire.core.app.ApplicationInfo
import app.campfire.core.app.Flavor
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import com.r0adkll.kimchi.annotations.ContributesTo
import io.ktor.client.HttpClient
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Provides

private val RESPONSE_CODE_REGEX = "RESPONSE: (\\d+)".toRegex(RegexOption.MULTILINE)

internal val HttpHeaders.RefreshToken: String get() = "x-refresh-token"
internal val HttpHeaders.ReturnTokens: String get() = "x-return-tokens"
internal val HttpHeaders.ServerUrl: String get() = "X-Server-Url"

@ContributesTo(AppScope::class)
interface HttpClientModule {

  @SingleIn(AppScope::class)
  @Provides
  fun provideHttpClient(
    applicationInfo: ApplicationInfo,
  ): HttpClient {
    return HttpClient {
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
          applicationInfo.debugBuild -> LogLevel.ALL
          applicationInfo.flavor == Flavor.Alpha -> LogLevel.INFO
          else -> LogLevel.NONE
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
}

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
          applicationInfo.debugBuild -> LogLevel.HEADERS
          applicationInfo.flavor == Flavor.Alpha -> LogLevel.INFO
          else -> LogLevel.NONE
        }
        logger = object : Logger {
          override fun log(message: String) {
            bark(
              tag = "KtorClient",
              priority = LogPriority.DEBUG,
              extras = mapOf(
                "isLogging" to "true",
              ),
            ) { message }
          }
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

private val ApplicationInfo.userAgent: String get() = buildString {
  // Append application name + Flavor
  append("Campfire")
  append(
    when (flavor) {
      Flavor.Beta -> " Beta"
      Flavor.Alpha -> " Alpha"
      else -> ""
    },
  )

  // Append application version
  append("/$versionName ")

  // Append OS information
  append("($osName $osVersion; Mobile)")
}

package app.campfire.network

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import com.r0adkll.kimchi.annotations.ContributesTo
import io.ktor.client.HttpClient
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Provides

@ContributesTo(AppScope::class)
interface HttpClientModule {

  @SingleIn(AppScope::class)
  @Provides
  fun provideHttpClient(): HttpClient {
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
        level = LogLevel.INFO
        logger = object : Logger {
          override fun log(message: String) {
            bark(LogPriority.INFO) { message }
          }
        }
      }
    }
  }
}

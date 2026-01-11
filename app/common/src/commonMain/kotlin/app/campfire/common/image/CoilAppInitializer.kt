package app.campfire.common.image

import app.campfire.account.api.AccountManager
import app.campfire.account.api.UserSessionManager
import app.campfire.core.app.AppInitializer
import app.campfire.core.di.AppScope
import app.campfire.core.session.userId
import app.campfire.network.asBearerTokens
import app.campfire.network.plugins.suspendingDefaultHeaders
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.header
import me.tatarka.inject.annotations.Inject

@ContributesMultibinding(AppScope::class)
@Inject
class CoilAppInitializer(
  private val userSessionManager: UserSessionManager,
  private val accountManager: AccountManager,
) : AppInitializer {

  override val priority: Int = AppInitializer.HIGHEST_PRIORITY

  override suspend fun onInitialize() {
    SingletonImageLoader.setSafe { context ->
      ImageLoader.Builder(context)
        .crossfade(true)
        .components {
          add(
            KtorNetworkFetcherFactory(
              httpClient = {
                authenticatingHttpClient()
              },
            ),
          )
        }
        .build()
    }
  }

  private fun authenticatingHttpClient(): HttpClient = HttpClient {
    install(Auth) {
      bearer {
        loadTokens {
          userSessionManager.current.userId?.let { userId ->
            accountManager.getToken(userId)?.asBearerTokens()
          }
        }
        sendWithoutRequest { true }
      }
    }

    suspendingDefaultHeaders {
      userSessionManager.current.userId?.let { userId ->
        val extraHeaders = accountManager.getExtraHeaders(userId)
        extraHeaders?.forEach { (name, value) -> header(name, value) }
      }
    }
  }
}

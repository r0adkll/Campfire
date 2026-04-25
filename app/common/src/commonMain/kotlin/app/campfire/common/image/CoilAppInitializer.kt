package app.campfire.common.image

import app.campfire.core.app.AppInitializer
import app.campfire.core.di.AppScope
import app.campfire.network.di.UserClient
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import io.ktor.client.HttpClient
import me.tatarka.inject.annotations.Inject

@ContributesMultibinding(AppScope::class)
@Inject
class CoilAppInitializer(
  @UserClient private val httpClient: Lazy<HttpClient>,
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
                httpClient.value
              },
            ),
          )
        }
        .build()
    }
  }
}

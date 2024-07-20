package app.campfire.network

import app.campfire.core.di.MergeAppScope
import com.r0adkll.kotlininject.merge.annotations.ContributesBinding
import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import me.tatarka.inject.annotations.Inject

@Inject
@ContributesBinding(MergeAppScope::class)
class KtorAudioBookShelfApi(
  private val httpClient: HttpClient,
) : AudioBookShelfApi {

  private val client by lazy {
    httpClient.config {
      defaultRequest {
      }
    }
  }
}

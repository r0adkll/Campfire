package app.campfire.auth

import app.campfire.auth.api.AuthRepository
import app.campfire.core.di.AppScope
import app.campfire.network.AudioBookShelfApi
import com.r0adkll.kotlininject.merge.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class)
@Inject
class DefaultAuthRepository(
  private val api: AudioBookShelfApi,
) : AuthRepository {

  override suspend fun healthCheck(serverUrl: String): Boolean {
    return api.ping(serverUrl)
  }

  override suspend fun authenticate(serverUrl: String, userName: String, password: String) {
    TODO("Not yet implemented")
  }
}

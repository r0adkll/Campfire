package app.campfire.account

import app.campfire.account.api.AccountManager
import app.campfire.account.storage.TokenStorage
import app.campfire.core.di.AppScope
import app.campfire.core.model.Server
import com.r0adkll.kotlininject.merge.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class)
@Inject
class DefaultAccountManager(
  private val tokenStorage: TokenStorage,
) : AccountManager {

  override suspend fun setToken(server: Server, token: String) {
    tokenStorage.put(server.url, token)
  }

  override suspend fun getToken(server: Server): String? {
    return tokenStorage.get(server.url)
  }
}

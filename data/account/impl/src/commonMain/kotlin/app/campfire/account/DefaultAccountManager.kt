package app.campfire.account

import app.campfire.account.api.AccountManager
import app.campfire.account.storage.TokenStorage
import app.campfire.core.di.AppScope
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class)
@Inject
class DefaultAccountManager(
  private val tokenStorage: TokenStorage,
) : AccountManager {

  override suspend fun setToken(serverUrl: String, token: String) {
    tokenStorage.put(serverUrl, token)
  }

  override suspend fun getToken(serverUrl: String): String? {
    return tokenStorage.get(serverUrl)
  }
}

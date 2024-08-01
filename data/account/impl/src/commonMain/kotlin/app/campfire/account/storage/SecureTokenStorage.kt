package app.campfire.account.storage

import app.campfire.core.di.AppScope
import com.r0adkll.kotlininject.merge.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class)
@Inject
class SecureTokenStorage(

) : TokenStorage {

  override suspend fun get(serverUrl: String): String {
    TODO("Not yet implemented")
  }

  override suspend fun put(serverUrl: String, token: String) {
    TODO("Not yet implemented")
  }
}

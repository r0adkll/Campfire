package app.campfire.account

import app.campfire.account.api.AccountManager
import app.campfire.core.di.AppScope
import app.campfire.core.model.Server
import com.r0adkll.kotlininject.merge.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class)
@Inject
class DefaultAccountManager(

) : AccountManager {

  override suspend fun setToken(server: Server, token: String) {
    TODO("Not yet implemented")
  }

  override suspend fun getToken(server: Server): String? {
    TODO("Not yet implemented")
  }
}

package app.campfire.account.session

import app.campfire.account.api.UserSessionManager
import app.campfire.account.api.di.UserGraphManager
import app.campfire.core.app.UserInitializer
import app.campfire.core.di.AppScope
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class)
@Inject
class UserSessionInitializer(
  private val userSessionRestorer: UserSessionRestorer,
  private val userSessionManager: UserSessionManager,
  private val userGraphManager: UserGraphManager,
) : UserInitializer {

  override suspend fun initialize() {
    val userSession = userSessionRestorer.restore()
    userGraphManager.create(userSession)
    userSessionManager.current = userSession
  }
}

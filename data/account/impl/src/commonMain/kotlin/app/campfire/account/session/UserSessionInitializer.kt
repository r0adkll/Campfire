package app.campfire.account.session

import app.campfire.account.api.UserSessionManager
import app.campfire.core.app.AppInitializer
import app.campfire.core.di.AppScope
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import me.tatarka.inject.annotations.Inject

@ContributesMultibinding(AppScope::class)
@Inject
class UserSessionInitializer(
  private val userSessionRestorer: UserSessionRestorer,
  private val userSessionManager: UserSessionManager,
) : AppInitializer {

  override val priority: Int = AppInitializer.HIGHEST_PRIORITY

  override suspend fun onInitialize() {
    userSessionManager.current = userSessionRestorer.restore()
  }
}

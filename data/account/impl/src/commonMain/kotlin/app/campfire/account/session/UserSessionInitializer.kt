package app.campfire.account.session

import app.campfire.account.api.UserSessionManager
import app.campfire.core.app.AppInitializer
import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

@ContributesIntoSet(AppScope::class)
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

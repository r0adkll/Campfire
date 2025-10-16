package app.campfire.account.session

import app.campfire.account.api.UserSessionManager
import app.campfire.account.api.di.UserGraphManager
import app.campfire.core.app.AppInitializer
import app.campfire.core.di.AppScope
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import me.tatarka.inject.annotations.Inject

@ContributesMultibinding(AppScope::class)
@Inject
class UserSessionInitializer(
  private val userSessionRestorer: UserSessionRestorer,
  private val userSessionManager: UserSessionManager,
  private val userGraphManager: UserGraphManager,
) : AppInitializer {

  // We limit the default max priority to [Int.MAX_VALUE - 10] by default
  // so the top 10 values are reserved system priorities. This initializer
  // should ALWAYS be the first to execute.
  // TODO: Write an integration test to ensure this is always true OR rewrite how these integrate in the
  //   main application to make it impossible.
  override val priority: Int = Int.MAX_VALUE

  override suspend fun onInitialize() {
    val userSession = userSessionRestorer.restore()
    userGraphManager.create(userSession)
    userSessionManager.current = userSession
  }
}

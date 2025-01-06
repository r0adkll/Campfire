package app.campfire.shared.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.SingleIn
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.session.UserSession
import com.r0adkll.kimchi.annotations.ContributesTo
import me.tatarka.inject.annotations.Inject

typealias UserSessionKey = String

@SingleIn(AppScope::class)
@Inject
class UserComponentManager(
  private val userComponentFactory: UserComponent.Factory,
) {

  /**
   * Containing cache of generated [UserComponent] graph objects. Due to the use
   * of switching UserComponent graph/states in the Compose layer configuration changes on
   * Android and anything else that causes the the entire composition to be destroyed / recreated
   * from caches will result in graph re-creations and relation breaking assumptions in other uses
   * and injections. i.e. in Services and so on. So we cache graph creations here that can be easily retrieved
   *
   */
  private val componentCache = mutableMapOf<UserSessionKey, UserComponent>()

  private var lastUserSession: UserSession? = null

  /**
   * Get the current cached [UserComponent] for a given session, or create a new
   * one if it doesn't exist.
   * @param userSession the user session that would key a [UserComponent]
   * @return the generated or cached [UserComponent] object graph
   */
  fun getOrCreateUserComponent(userSession: UserSession): UserComponent {
    cancelCurrentScope()
    lastUserSession = userSession
    val cached = componentCache[userSession.cacheKey]
    if (cached != null) {
      bark(LogPriority.INFO) { "Cached UserComponent for $userSession found" }
      return cached
    } else {
      bark(LogPriority.INFO) { "No cached UserComponent found for $userSession" }
      val newUserComponent = userComponentFactory.create(userSession)
      componentCache[userSession.cacheKey] = newUserComponent
      return newUserComponent
    }
  }

  private fun cancelCurrentScope() {
    lastUserSession?.let { session ->
      bark { "Cancelling UserComponent scope for $session" }
      componentCache[session.key]?.coroutineScopeHolder?.cancel()
    }
  }

  private val UserSession.cacheKey: UserSessionKey
    get() = key.toString()
}

@ContributesTo(AppScope::class)
interface UserComponentManagerComponent {
  val userComponentManager: UserComponentManager
}

@Composable
internal fun rememberUserComponentManager(): UserComponentManager {
  return remember { ComponentHolder.component<UserComponentManagerComponent>().userComponentManager }
}

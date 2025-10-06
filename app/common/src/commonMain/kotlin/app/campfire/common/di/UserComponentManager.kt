package app.campfire.common.di

import app.campfire.account.api.di.UserGraphManager
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.SingleIn
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.session.UserSession
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import me.tatarka.inject.annotations.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class UserComponentManager(
  private val userComponentFactory: UserComponent.Factory,
  @ForScope(AppScope::class) private val applicationScope: CoroutineScope,
) : UserGraphManager {

  private val coroutineExceptionHandler = CoroutineExceptionHandler { context, throwable ->
    bark(LogPriority.ERROR, throwable = throwable) { "Coroutine Exception in UserComponentManager" }
  }

  override fun create(userSession: UserSession) {
    val newUserComponent = userComponentFactory.create(userSession)
    ComponentHolder.updateComponent(applicationScope, newUserComponent)
  }

  override suspend fun destroy() {
    val userComponent = ComponentHolder.component<UserComponent>()

    withContext(applicationScope.coroutineContext + coroutineExceptionHandler) {
      userComponent.scopedDependencies.value
        .map { scoped ->
          bark { "Destroying: $scoped" }
          async {
            withTimeoutOrNull(150L) {
              scoped.onDestroy()
            }
          }
        }
        .awaitAll()
    }

    // Cancel the scope!
    bark { "Tearing down UserScope coroutine scope" }
    userComponent.coroutineScopeHolder.cancel("UserScope destroyed")
  }
}

package app.campfire.socket.impl

import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.Scoped
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.Corked
import app.campfire.socket.SocketManager
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@ContributesMultibinding(UserScope::class, boundType = Scoped::class)
@Inject
class SocketEventLogger(
  private val socketManager: SocketManager,
  @ForScope(UserScope::class) private val coroutineScopeHolder: CoroutineScopeHolder,
) : Scoped {

  companion object : Corked("SocketEventLogger")

  override suspend fun onCreate() {
    coroutineScopeHolder.get().launch {
      socketManager.events.collect { event ->
        ibark { "Socket event: $event" }
      }
    }
  }
}

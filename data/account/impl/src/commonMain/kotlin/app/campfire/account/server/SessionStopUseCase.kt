package app.campfire.account.server

import app.campfire.audioplayer.PlaybackController
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.UserScope
import app.campfire.sessions.api.SessionsRepository
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull

interface SessionStopUseCase {

  suspend fun execute()
}

@ContributesTo(UserScope::class)
interface SessionStopUserComponent {
  val sessionRepository: SessionsRepository
  val playbackController: PlaybackController
}

@ContributesBinding(AppScope::class)
@Inject
class DefaultSessionStopUseCase : SessionStopUseCase {

  private val userComponent: SessionStopUserComponent
    get() = ComponentHolder.component()

  override suspend fun execute() {
    // Check for any current sessions and stop them
    val currentSession = userComponent.sessionRepository.getCurrentSession()
    if (currentSession != null) {
      userComponent.playbackController
        .stopSession(currentSession.libraryItem.id)
    }

    // Force wait until current session is null
    userComponent.sessionRepository
      .observeCurrentSession()
      .filter { it == null }
      .firstOrNull()
  }
}

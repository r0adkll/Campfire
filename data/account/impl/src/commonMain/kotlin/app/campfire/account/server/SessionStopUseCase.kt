package app.campfire.account.server

import app.campfire.audioplayer.PlaybackController
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.UserScope
import app.campfire.sessions.api.SessionsRepository
import com.r0adkll.kimchi.annotations.ContributesBinding
import com.r0adkll.kimchi.annotations.ContributesTo
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import me.tatarka.inject.annotations.Inject

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

package app.campfire.account.server

import app.campfire.audioplayer.PlaybackController
import app.campfire.core.di.Scoped
import app.campfire.core.di.UserScope
import app.campfire.sessions.api.SessionsRepository
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.timeout
import me.tatarka.inject.annotations.Inject

@ContributesMultibinding(UserScope::class, boundType = Scoped::class)
@Inject
class SessionStopUseCase(
  private val sessionRepository: SessionsRepository,
  private val playbackController: PlaybackController,
) : Scoped {

  @OptIn(FlowPreview::class)
  override suspend fun onDestroy() {
    // Check for any current sessions and stop them
    val currentSession = sessionRepository.getCurrentSession()
    if (currentSession != null) {
      playbackController.stopSession(currentSession.libraryItem.id)

      // Force wait until current session is null
      sessionRepository
        .observeCurrentSession()
        .filter { it == null }
        .onEmpty { emit(null) }
        .timeout(1.seconds)
        .firstOrNull()
    }
  }
}

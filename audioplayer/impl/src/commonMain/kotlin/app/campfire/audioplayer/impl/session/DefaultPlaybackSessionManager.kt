package app.campfire.audioplayer.impl.session

import app.campfire.audioplayer.PlaybackController
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItemId
import app.campfire.sessions.api.SessionsRepository
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class DefaultPlaybackSessionManager(
  private val sessionsRepository: SessionsRepository,
  private val playbackController: PlaybackController,
  private val dispatcherProvider: DispatcherProvider,
) : PlaybackSessionManager {

  private var currentSessionUpdater: Deferred<Unit>? = null

  override suspend fun startSession(
    libraryItemId: LibraryItemId,
    playImmediately: Boolean,
  ) {
    withContext(dispatcherProvider.io) {
      val session = sessionsRepository.createSession(libraryItemId)

      bark("AudioPlayer") { "Preparing playback session: $session" }

      val player = playbackController.currentPlayer.value
        ?: throw IllegalStateException("There isn't a media player available, unable to prepare session")
      player.prepare(session, playImmediately)

      // Now observe the current session to upd
      currentSessionUpdater?.cancel()
      currentSessionUpdater = async {
        player.overallTime
          .onCompletion {
            bark("AudioPlayer", LogPriority.INFO) { "Finished session updater" }
          }
          .collect { time ->
            bark("AudioPlayer", LogPriority.VERBOSE) { "Updating Session @ $time" }
            sessionsRepository.updateSession(libraryItemId, time)
          }
      }
    }
  }

  override suspend fun stopSession(libraryItemId: LibraryItemId) {
    currentSessionUpdater?.cancel()
    currentSessionUpdater = null

    bark("AudioPlayer") { "Stopping playback session for $libraryItemId" }
    sessionsRepository.stopSession(libraryItemId)
  }
}

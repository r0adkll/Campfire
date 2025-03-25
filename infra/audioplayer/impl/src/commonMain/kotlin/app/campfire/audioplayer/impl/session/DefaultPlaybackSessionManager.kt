package app.campfire.audioplayer.impl.session

import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItemId
import app.campfire.sessions.api.SessionsRepository
import app.campfire.user.api.MediaProgressRepository
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class DefaultPlaybackSessionManager(
  private val sessionsRepository: SessionsRepository,
  private val mediaProgressRepository: MediaProgressRepository,
  private val audioPlayerHolder: AudioPlayerHolder,
  private val dispatcherProvider: DispatcherProvider,
) : PlaybackSessionManager {

  override suspend fun startSession(
    libraryItemId: LibraryItemId,
    playImmediately: Boolean,
    chapterId: Int?,
  ) {
    withContext(dispatcherProvider.io) {
      val session = sessionsRepository.createSession(libraryItemId)
      bark("AudioPlayer") { "Preparing playback session: $session" }

      val player = audioPlayerHolder.currentPlayer.value
        ?: throw IllegalStateException("There isn't a media player available, unable to prepare session")
      player.prepare(session, playImmediately, chapterId) { libraryItemId ->
        // TODO: We should probably wire this into some sort of playlist functionality
        //  where we want to mark the finished item as done, and start the next.
        mediaProgressRepository.markFinished(libraryItemId)
      }
    }
  }

  override suspend fun stopSession(libraryItemId: LibraryItemId) {
    bark("AudioPlayer") { "Stopping playback session for $libraryItemId" }
    sessionsRepository.stopSession(libraryItemId)
  }
}

package app.campfire.audioplayer.impl.session

import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.logging.Corked
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.loggableId
import app.campfire.sessions.api.SessionQueue
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
  private val sessionQueue: SessionQueue,
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
      ibark { "Preparing playback session for ${libraryItemId.loggableId}: ${session.id}" }

      val player = audioPlayerHolder.currentPlayer.value
        ?: throw IllegalStateException("There isn't a media player available, unable to prepare session")

      player.prepare(session, playImmediately, chapterId) { libraryItemId ->
        mediaProgressRepository.markFinished(libraryItemId)

        // Check if we have an item next in the queue
        val nextItem = sessionQueue.pop()
        if (nextItem != null) {
          // Kick off the next item by calling this very function.
          startSession(nextItem.id, playImmediately = true)
        } else {
          // If we don't have a next-of-queue, Mark the session as finished which maxes out its current time
          // and marks it as inactive so it can be sync'd and then deleted
          sessionsRepository.markFinished(session.libraryItem.id)
        }
      }
    }
  }

  override suspend fun stopSession(libraryItemId: LibraryItemId) {
    ibark { "Stopping playback session for ${libraryItemId.loggableId}" }
    sessionsRepository.stopSession(libraryItemId)
    sessionQueue.clear()
  }

  companion object : Corked("PlaybackSessionManager")
}

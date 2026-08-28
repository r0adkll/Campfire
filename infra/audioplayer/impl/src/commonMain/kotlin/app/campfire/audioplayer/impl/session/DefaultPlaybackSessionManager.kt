// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.session

import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.audioplayer.history.PlaybackHistoryRepository
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.logging.Corked
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlayMethod
import app.campfire.core.model.PodcastEpisodeId
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
  private val playbackHistoryRepository: PlaybackHistoryRepository,
  private val audioPlayerHolder: AudioPlayerHolder,
  private val dispatcherProvider: DispatcherProvider,
) : PlaybackSessionManager {

  override suspend fun startSession(
    libraryItemId: LibraryItemId,
    playImmediately: Boolean,
    chapterId: Int?,
    episodeId: PodcastEpisodeId?,
    methodOverride: PlayMethod?,
  ) {
    withContext(dispatcherProvider.io) {
      val session = sessionsRepository.createSession(libraryItemId, episodeId, methodOverride)

      // eBook-only items have no audio to prepare. Guarded here (not just in the UI) so
      // media browser entry points like Android Auto can't hand the player an empty track list.
      if (session.libraryItem.isEbookOnly) {
        wbark { "Refusing playback session for ebook-only item ${libraryItemId.loggableId}" }
        sessionsRepository.markDeleted(libraryItemId, episodeId)
        return@withContext
      }

      ibark { "Preparing playback session for ${libraryItemId.loggableId}: ${session.id}" }

      val player = audioPlayerHolder.currentPlayer.value
        ?: throw IllegalStateException("There isn't a media player available, unable to prepare session")

      // Remove this item from the queue, if exists. Scope to the same episode for podcasts
      // so other queued episodes of the same podcast aren't clobbered.
      sessionQueue.remove(session.libraryItem.id, session.episodeId)

      player.prepare(session, playImmediately, chapterId) { libraryItemId ->
        // For podcast sessions, scope all the finishing operations to the playing episode
        // so siblings (other episodes' progress / history / sessions) aren't clobbered.
        mediaProgressRepository.markFinished(libraryItemId, session.episodeId)
        playbackHistoryRepository.clear(libraryItemId, session.episodeId)

        // Check if we have an item next in the queue
        val nextItem = sessionQueue.pop()
        if (nextItem != null) {
          // Kick off the next item by calling this very function.
          startSession(
            libraryItemId = nextItem.libraryItemId,
            playImmediately = true,
            episodeId = nextItem.episodeId,
          )
        } else {
          // If we don't have a next-of-queue, Mark the session as finished which maxes out its current time
          // and marks it as inactive so it can be sync'd and then deleted
          sessionsRepository.markFinished(session.libraryItem.id, session.episodeId)
        }
      }
    }
  }

  override suspend fun stopSession(
    libraryItemId: LibraryItemId,
    clearQueue: Boolean,
    episodeId: PodcastEpisodeId?,
  ) {
    ibark { "Stopping playback session for ${libraryItemId.loggableId}" }

    if (clearQueue) {
      sessionsRepository.stopSession(libraryItemId, episodeId)
      sessionQueue.clear()
    } else {
      // If the item is the current playing item (and the same episode for podcasts),
      // pop the queue and start playing the next item.
      val current = sessionsRepository.getCurrentSession()
      val isCurrent = current?.libraryItem?.id == libraryItemId &&
        current.episodeId == episodeId
      if (isCurrent) {
        val nextItem = sessionQueue.pop()
        if (nextItem != null) {
          startSession(
            libraryItemId = nextItem.libraryItemId,
            playImmediately = true,
            episodeId = nextItem.episodeId,
          )
        } else {
          sessionsRepository.stopSession(libraryItemId, episodeId)
        }
      }
    }
  }

  companion object : Corked("PlaybackSessionManager")
}

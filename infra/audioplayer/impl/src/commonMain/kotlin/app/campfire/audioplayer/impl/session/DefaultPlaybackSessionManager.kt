// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.session

import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.audioplayer.history.PlaybackHistoryRepository
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.logging.Corked
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlayMethod
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.core.model.Session
import app.campfire.core.model.loggableId
import app.campfire.offline.DownloadContent
import app.campfire.offline.Downloads
import app.campfire.sessions.api.SessionQueue
import app.campfire.sessions.api.SessionsRepository
import app.campfire.settings.api.DownloadSettings
import app.campfire.settings.api.PlaybackSettings
import app.campfire.user.api.MediaProgressRepository
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.withContext

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class DefaultPlaybackSessionManager(
  private val sessionsRepository: SessionsRepository,
  private val sessionQueue: SessionQueue,
  private val mediaProgressRepository: MediaProgressRepository,
  private val playbackHistoryRepository: PlaybackHistoryRepository,
  private val audioPlayerHolder: AudioPlayerHolder,
  private val playbackSettings: PlaybackSettings,
  private val downloads: Downloads,
  private val downloadSettings: DownloadSettings,
  private val dispatcherProvider: DispatcherProvider,
) : PlaybackSessionManager {

  override suspend fun startSession(
    libraryItemId: LibraryItemId,
    playImmediately: Boolean,
    chapterId: Int?,
    episodeId: PodcastEpisodeId?,
    methodOverride: PlayMethod?,
    requeueCurrent: Boolean,
  ) {
    withContext(dispatcherProvider.io) {
      // Read before creating the new session, which deactivates every other one
      val requeue = requeueCurrent && playbackSettings.requeueOnSwitch
      val displaced = if (requeue) sessionsRepository.getCurrentSession() else null
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

      displaced?.let { requeueDisplaced(it, replacement = session) }

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
            requeueCurrent = false,
          )
        } else {
          // If we don't have a next-of-queue, Mark the session as finished which maxes out its current time
          // and marks it as inactive so it can be sync'd and then deleted
          sessionsRepository.markFinished(session.libraryItem.id, session.episodeId)
        }

        // Last, so any next item has already taken the player off this one's file
        deleteFinishedDownload(session)
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
            requeueCurrent = false,
          )
        } else {
          sessionsRepository.stopSession(libraryItemId, episodeId)
        }
      }
    }
  }

  /**
   * Moves the book or podcast episode [replacement] takes the player from to the front of the
   * queue, so it plays next instead of dropping out. Items marked finished or reset from their
   * screens (deleted sessions) stay out.
   */
  private suspend fun requeueDisplaced(displaced: Session, replacement: Session) {
    if (displaced.isDeleted) return
    val isSameEntry = displaced.libraryItem.id == replacement.libraryItem.id &&
      displaced.episodeId == replacement.episodeId
    if (isSameEntry) return

    // An episode no longer on its podcast can't be queued; without it the podcast would be
    // queued as if it were a book
    val episode = displaced.episode
    if (displaced.episodeId != null && episode == null) return

    ibark { "Requeueing displaced session ${displaced.libraryItem.id.loggableId}" }
    sessionQueue.addFirst(displaced.libraryItem, episode)
  }

  /**
   * Removes the download of the podcast episode [finished] played to the end, while "Remove
   * finished episodes" is on. Books keep theirs.
   */
  private fun deleteFinishedDownload(finished: Session) {
    val episodeId = finished.episodeId ?: return
    if (!downloadSettings.deleteFinishedEpisodes) return

    ibark { "Removing the download of finished episode ${episodeId.loggableId}" }
    downloads.delete(DownloadContent.Episode(finished.libraryItem.id, episodeId))
  }

  companion object : Corked("PlaybackSessionManager")
}

package app.campfire.sessions.api

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.core.model.Session
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow

interface SessionsRepository {

  suspend fun getSession(libraryItemId: LibraryItemId): Session?

  suspend fun getCurrentSession(): Session?

  /**
   * Create a new listening session to begin playback
   * @param libraryItemId The item to begin listening to
   * @param episodeId Optional podcast episode id when [libraryItemId] points at a podcast item
   * @return The newly created session
   */
  suspend fun createSession(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId? = null,
  ): Session

  /**
   * Mark a session for deletion on next sync, deleting it instead of
   * sync'ing it with the server. Pass [episodeId] to scope the operation to a podcast
   * episode session — the row is only marked deleted when its stored episodeId matches.
   *
   * @param libraryItemId The id of the session to mark for deletion
   * @param episodeId Optional podcast episode id to scope the deletion
   */
  suspend fun markDeleted(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId? = null,
  )

  /**
   * Delete a listening session and discard the media progress locally and remotely for the
   * item.
   *
   * @param libraryItemId The id of the session to delete
   * @param episodeId Optional podcast episode id to scope the deletion
   */
  suspend fun deleteSession(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId? = null,
  )

  /**
   * Update a current listening session
   * @param libraryItemId the id of the item to update
   * @param currentTime the current time to update
   */
  suspend fun updateCurrentTime(
    libraryItemId: LibraryItemId,
    currentTime: Duration,
  )

  /**
   * Update the last time the item was played, this is used
   * for synchronizing the session against external progress updates
   */
  suspend fun updateLastPlayed(
    libraryItemId: LibraryItemId,
  )

  /**
   * Add the [amount] of time to the timeListening for the current [libraryItemId] session
   * @param libraryItemId the id of the session to update
   * @param amount the amount of time to add to the cumulative listening time
   */
  suspend fun addTimeListening(
    libraryItemId: LibraryItemId,
    amount: Duration,
  )

  /**
   * Stop the current active session, if one exists, and mark it to be deleted.
   * After the stopped session is synchronized to the server, it will be removed.
   *
   * If a new session for the same libraryItemId is started matching this session,
   * then a new one will replace it, using the mediaProgress to configure its active time
   * and the stopped session will NOT be synced to the server.
   *
   * @param libraryItemId the id of the session to stop
   * @param episodeId Optional podcast episode id to scope the stop
   */
  suspend fun stopSession(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId? = null,
  )

  /**
   * Mark a session as finished, remove it from active and let it be sync to the backend and
   * then removed.
   * @param libraryItemId the id of the session to mark as finished
   * @param episodeId Optional podcast episode id to scope the finish
   */
  suspend fun markFinished(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId? = null,
  )

  /**
   * Observe the current listening session if one exists
   * @return A flow of the current listening session
   */
  fun observeCurrentSession(): Flow<Session?>
}

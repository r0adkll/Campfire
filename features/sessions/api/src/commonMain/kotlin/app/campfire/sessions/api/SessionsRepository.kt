package app.campfire.sessions.api

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.Session
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow

interface SessionsRepository {

  suspend fun getSession(libraryItemId: LibraryItemId): Session?

  suspend fun getCurrentSession(): Session?

  /**
   * Create a new listening session to begin playback
   * @param item The item to begin listening to
   * @return The newly created session
   */
  suspend fun createSession(libraryItemId: LibraryItemId): Session

  /**
   * Delete a listening session and discard the media progress locally and remotely for the
   * item.
   *
   * @param libraryItemId The id of the session to delete
   */
  suspend fun deleteSession(libraryItemId: LibraryItemId)

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
   * Add the [amount] of time to the timeListening for the current [libraryItemId] session
   * @param libraryItemId the id of the session to update
   * @param amount the amount of time to add to the cumulative listening time
   */
  suspend fun addTimeListening(
    libraryItemId: LibraryItemId,
    amount: Duration,
  )

  /**
   * Stop the current active session, if one exists
   * @param libraryItemId the id of the session to stop
   */
  suspend fun stopSession(
    libraryItemId: LibraryItemId,
  )

  /**
   * Mark a session as finished, remove it from active and let it be sync to the backend and
   * then removed.
   * @param libraryItemId the id of the session to mark as finished
   */
  suspend fun markFinished(
    libraryItemId: LibraryItemId,
  )

  /**
   * Observe the current listening session if one exists
   * @return A flow of the current listening session
   */
  fun observeCurrentSession(): Flow<Session?>
}

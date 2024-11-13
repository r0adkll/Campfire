package app.campfire.sessions.api

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.Session
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow

interface SessionsRepository {

  /**
   * Create a new listening session to begin playback
   * @param item The item to begin listening to
   * @return The newly created session
   */
  suspend fun createSession(libraryItemId: LibraryItemId): Session

  /**
   * Delete a listening session
   * @param libraryItemId The id of the session to delete
   */
  suspend fun deleteSession(libraryItemId: LibraryItemId)

  /**
   * Update a current listening session
   * @param libraryItemId the id of the item to update
   * @param currentTime the current time to update
   */
  suspend fun updateSession(
    libraryItemId: LibraryItemId,
    currentTime: Duration,
  )

  /**
   * Stop the current active session, if one exists
   * @param libraryItemId the id of the session to stop
   */
  suspend fun stopSession(
    libraryItemId: LibraryItemId,
  )

  /**
   * Observe the current listening session if one exists
   * @return A flow of the current listening session
   */
  fun observeCurrentSession(): Flow<Session?>
}

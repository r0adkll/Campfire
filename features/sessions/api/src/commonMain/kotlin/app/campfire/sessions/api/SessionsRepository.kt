package app.campfire.sessions.api

import app.campfire.core.model.LibraryItem
import app.campfire.sessions.api.models.Session
import app.campfire.sessions.api.models.SessionId
import kotlinx.coroutines.flow.Flow

interface SessionsRepository {

  /**
   * Create a new listening session to begin playback
   * @param item The item to begin listening to
   * @return The newly created session
   */
  suspend fun createSession(item: LibraryItem): Session

  /**
   * Delete a listening session
   * @param sessionId The id of the session to delete
   */
  suspend fun deleteSession(sessionId: SessionId)

  /**
   * Observe the current listening session if one exists
   * @return A flow of the current listening session
   */
  fun observeCurrentSession(): Flow<Session?>
}

package app.campfire.sessions.db

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.PlayMethod
import app.campfire.core.model.Session
import app.campfire.core.model.UserId
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow

interface SessionDataSource {

  fun observeCurrentSession(): Flow<Session?>

  suspend fun getCurrentSession(): Session?

  suspend fun getSession(libraryItemId: LibraryItemId): Session?

  suspend fun getSessions(userId: UserId): List<Session>

  suspend fun createOrStartSession(
    libraryItemId: LibraryItemId,
    playMethod: PlayMethod,
    progress: MediaProgress?,
  ): Session

  suspend fun updateCurrentTime(
    libraryItemId: LibraryItemId,
    currentTime: Duration,
  )

  suspend fun updateLastPlayed(
    libraryItemId: LibraryItemId,
  )

  suspend fun addTimeListening(
    libraryItemId: LibraryItemId,
    amount: Duration,
  )

  suspend fun markDeleted(
    libraryItemId: LibraryItemId,
  )

  suspend fun deleteSession(
    libraryItemId: LibraryItemId,
  )

  suspend fun stopSession(
    libraryItemId: LibraryItemId,
  )

  suspend fun markFinished(
    libraryItemId: LibraryItemId,
  )
}

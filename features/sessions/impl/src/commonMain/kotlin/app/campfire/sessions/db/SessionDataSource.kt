package app.campfire.sessions.db

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlayMethod
import app.campfire.core.model.Session
import app.campfire.core.model.UserId
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

interface SessionDataSource {

  fun observeCurrentSession(): Flow<Session?>

  suspend fun getCurrentSession(): Session?

  suspend fun getSession(libraryItemId: LibraryItemId): Session?

  suspend fun getSessions(userId: UserId): List<Session>

  suspend fun createOrStartSession(
    libraryItemId: LibraryItemId,
    playMethod: PlayMethod,
    mediaPlayer: String,
    duration: Duration,
    currentTime: Duration,
    startedAt: LocalDateTime,
    forceNew: Boolean = false,
  ): Session

  suspend fun updateCurrentTime(
    libraryItemId: LibraryItemId,
    currentTime: Duration,
  )

  suspend fun addTimeListening(
    libraryItemId: LibraryItemId,
    amount: Duration,
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

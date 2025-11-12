package app.campfire.sessions.test

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.Session
import app.campfire.sessions.api.SessionsRepository
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSessionsRepository : SessionsRepository {

  val invocations = mutableListOf<Invocation>()

  var session: Session? = null
  override suspend fun getSession(libraryItemId: LibraryItemId): Session? {
    invocations += Invocation.GetSession(libraryItemId)
    return session
  }

  var currentSession: Session? = null
  override suspend fun getCurrentSession(): Session? {
    invocations += Invocation.CurrentSession
    return currentSession
  }

  lateinit var createSession: Session
  override suspend fun createSession(libraryItemId: LibraryItemId): Session {
    invocations += Invocation.CreateSession(libraryItemId)
    return createSession
  }

  override suspend fun deleteSession(libraryItemId: LibraryItemId) {
    invocations += Invocation.DeleteSession(libraryItemId)
  }

  override suspend fun updateCurrentTime(
    libraryItemId: LibraryItemId,
    currentTime: Duration,
  ) {
    invocations += Invocation.UpdateCurrentTime(libraryItemId, currentTime)
  }

  override suspend fun addTimeListening(
    libraryItemId: LibraryItemId,
    amount: Duration,
  ) {
    invocations += Invocation.AddTimeListening(libraryItemId, amount)
  }

  override suspend fun stopSession(libraryItemId: LibraryItemId) {
    invocations += Invocation.StopSession(libraryItemId)
  }

  val currentSessionFlow = MutableStateFlow<Session?>(null)
  override fun observeCurrentSession(): Flow<Session?> {
    invocations += Invocation.ObserveCurrentSession
    return currentSessionFlow
  }

  sealed interface Invocation {
    data class GetSession(val libraryItemId: LibraryItemId) : Invocation
    data object CurrentSession : Invocation
    data class CreateSession(val libraryItemId: LibraryItemId) : Invocation
    data class DeleteSession(val libraryItemId: LibraryItemId) : Invocation
    data class UpdateCurrentTime(val libraryItemId: LibraryItemId, val currentTime: Duration) : Invocation
    data class AddTimeListening(val libraryItemId: LibraryItemId, val amount: Duration) : Invocation
    data class StopSession(val libraryItemId: LibraryItemId) : Invocation
    data object ObserveCurrentSession : Invocation
  }
}

package app.campfire.sessions.test

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PodcastEpisodeId
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
  override suspend fun createSession(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
  ): Session {
    invocations += Invocation.CreateSession(libraryItemId, episodeId)
    return createSession
  }

  override suspend fun markDeleted(libraryItemId: LibraryItemId, episodeId: PodcastEpisodeId?) {
    invocations += Invocation.MarkDeleted(libraryItemId, episodeId)
  }

  override suspend fun deleteSession(libraryItemId: LibraryItemId, episodeId: PodcastEpisodeId?) {
    invocations += Invocation.DeleteSession(libraryItemId, episodeId)
  }

  override suspend fun updateCurrentTime(
    libraryItemId: LibraryItemId,
    currentTime: Duration,
  ) {
    invocations += Invocation.UpdateCurrentTime(libraryItemId, currentTime)
  }

  override suspend fun updateLastPlayed(
    libraryItemId: LibraryItemId,
  ) {
    invocations += Invocation.UpdateLastPlayed(libraryItemId)
  }

  override suspend fun addTimeListening(
    libraryItemId: LibraryItemId,
    amount: Duration,
  ) {
    invocations += Invocation.AddTimeListening(libraryItemId, amount)
  }

  override suspend fun stopSession(libraryItemId: LibraryItemId, episodeId: PodcastEpisodeId?) {
    invocations += Invocation.StopSession(libraryItemId, episodeId)
  }

  override suspend fun markFinished(libraryItemId: LibraryItemId, episodeId: PodcastEpisodeId?) {
    invocations += Invocation.MarkFinished(libraryItemId, episodeId)
  }

  val currentSessionFlow = MutableStateFlow<Session?>(null)
  override fun observeCurrentSession(): Flow<Session?> {
    invocations += Invocation.ObserveCurrentSession
    return currentSessionFlow
  }

  sealed interface Invocation {
    data class GetSession(val libraryItemId: LibraryItemId) : Invocation
    data object CurrentSession : Invocation
    data class CreateSession(
      val libraryItemId: LibraryItemId,
      val episodeId: PodcastEpisodeId? = null,
    ) : Invocation
    data class MarkDeleted(
      val libraryItemId: LibraryItemId,
      val episodeId: PodcastEpisodeId? = null,
    ) : Invocation
    data class DeleteSession(
      val libraryItemId: LibraryItemId,
      val episodeId: PodcastEpisodeId? = null,
    ) : Invocation
    data class UpdateCurrentTime(val libraryItemId: LibraryItemId, val currentTime: Duration) : Invocation
    data class UpdateLastPlayed(val libraryItemId: LibraryItemId) : Invocation
    data class AddTimeListening(val libraryItemId: LibraryItemId, val amount: Duration) : Invocation
    data class StopSession(
      val libraryItemId: LibraryItemId,
      val episodeId: PodcastEpisodeId? = null,
    ) : Invocation
    data class MarkFinished(
      val libraryItemId: LibraryItemId,
      val episodeId: PodcastEpisodeId? = null,
    ) : Invocation
    data object ObserveCurrentSession : Invocation
  }
}

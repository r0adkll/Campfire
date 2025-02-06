package app.campfire.sessions.db

import app.campfire.CampfireDatabase
import app.campfire.account.api.UserSessionManager
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlayMethod
import app.campfire.core.model.Session
import app.campfire.core.model.UserId
import app.campfire.core.session.UserSession
import app.campfire.core.session.requiredUserId
import app.campfire.core.session.userId
import app.campfire.core.time.FatherTime
import app.campfire.data.Session as DbSession
import app.campfire.libraries.api.LibraryItemRepository
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import me.tatarka.inject.annotations.Inject

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class SqlDelightSessionDataSource(
  private val userSession: UserSession,
  private val db: CampfireDatabase,
  private val fatherTime: FatherTime,
  private val userSessionManager: UserSessionManager,
  private val libraryItemRepository: LibraryItemRepository,
  private val dispatcherProvider: DispatcherProvider,
) : SessionDataSource {

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeCurrentSession(): Flow<Session?> {
    return userSessionManager.observe()
      .filterIsInstance<UserSession.LoggedIn>()
      .flatMapLatest { userSession ->
        db.sessionQueries
          .getActive(userSession.user.id)
          .asFlow()
          .mapToOneOrNull(dispatcherProvider.databaseRead)
          .map {
            it?.let { model -> hydrateSession(model) }
          }
      }
  }

  override suspend fun getCurrentSession(): Session? {
    val currentUserId = userSessionManager.current.userId ?: return null
    return db.sessionQueries.getActive(currentUserId)
      .awaitAsOneOrNull()
      ?.let { hydrateSession(it) }
  }

  override suspend fun getSession(libraryItemId: LibraryItemId): Session? {
    return withContext(dispatcherProvider.databaseRead) {
      db.sessionQueries.getForId(libraryItemId, userSession.requiredUserId)
        .executeAsOneOrNull()
        ?.let { hydrateSession(it) }
    }
  }

  override suspend fun getSessions(userId: UserId): List<Session> {
    return withContext(dispatcherProvider.databaseRead) {
      db.sessionQueries.getAll(userId)
        .awaitAsList()
        .map { hydrateSession(it) }
    }
  }

  /**
   * Sessions should be pretty ephemeral and more/less synced to the user experience.
   * Anytime we call this method, essentially when the user opens the app again and auto-loads
   * or starts a new listening session then we'll want to replace the current db entry that
   * has a new id and reset timeListening.
   *
   * This helps provide better accuracy and reporting on the backend for the user stats
   */
  override suspend fun createOrStartSession(
    libraryItemId: LibraryItemId,
    playMethod: PlayMethod,
    mediaPlayer: String,
    duration: Duration,
    currentTime: Duration,
    startedAt: LocalDateTime,
  ): Session {
    val currentUserId = userSession.requiredUserId

    return withContext(dispatcherProvider.databaseWrite) {
      val dbSession = DbSession(
        id = Uuid.random(),
        userId = currentUserId,
        libraryItemId = libraryItemId,
        isActive = true,
        playMethod = PlayMethod.DirectPlay,
        mediaPlayer = "campfire",
        timeListening = 0.seconds,
        startTime = currentTime,
        currentTime = currentTime,
        startedAt = fatherTime.now(),
        updatedAt = fatherTime.now(),
      )

      // Insert, replacing any existing session and disable any other active sessions
      db.transaction {
        db.sessionQueries.insert(dbSession)
        db.sessionQueries.activate(currentUserId, libraryItemId)
      }

      // Hydrate with latest item
      hydrateSession(dbSession)
    }
  }

  override suspend fun updateCurrentTime(libraryItemId: LibraryItemId, currentTime: Duration) {
    withContext(dispatcherProvider.databaseWrite) {
      // Update the playback session information with the new time
      db.sessionQueries.updatePlayback(
        libraryItemId = libraryItemId,
        userId = userSession.requiredUserId,
        currentTime = currentTime,
        updatedAt = fatherTime.now(),
      )
    }
  }

  override suspend fun addTimeListening(libraryItemId: LibraryItemId, amount: Duration) {
    withContext(dispatcherProvider.databaseWrite) {
      db.sessionQueries.addTimeListening(
        libraryItemId = libraryItemId,
        userId = userSession.requiredUserId,
        timeListening = amount,
        updatedAt = fatherTime.now(),
      )
    }
  }

  override suspend fun deleteSession(libraryItemId: LibraryItemId) {
    withContext(dispatcherProvider.databaseWrite) {
      db.sessionQueries.delete(libraryItemId, userSession.requiredUserId)
    }
  }

  override suspend fun stopSession(libraryItemId: LibraryItemId) {
    withContext(dispatcherProvider.databaseWrite) {
      db.sessionQueries.disable(libraryItemId, userSession.requiredUserId)
    }
  }

  private suspend fun hydrateSession(session: DbSession): Session {
    val libraryItem = libraryItemRepository.getLibraryItem(session.libraryItemId)
    return Session(
      id = session.id,
      userId = session.userId,
      libraryItem = libraryItem,
      playMethod = session.playMethod,
      mediaPlayer = session.mediaPlayer,
      timeListening = session.timeListening,
      startTime = session.startTime,
      currentTime = session.currentTime,
      startedAt = session.startedAt,
      updatedAt = session.updatedAt,
    )
  }
}

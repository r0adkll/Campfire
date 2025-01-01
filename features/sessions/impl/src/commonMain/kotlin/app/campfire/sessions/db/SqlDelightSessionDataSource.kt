package app.campfire.sessions.db

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlayMethod
import app.campfire.core.model.Session
import app.campfire.core.time.FatherTime
import app.campfire.data.Session as DbSession
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.user.api.UserRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import me.tatarka.inject.annotations.Inject

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class SqlDelightSessionDataSource(
  private val db: CampfireDatabase,
  private val fatherTime: FatherTime,
  private val userRepository: UserRepository,
  private val libraryItemRepository: LibraryItemRepository,
  private val dispatcherProvider: DispatcherProvider,
) : SessionDataSource {

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeCurrentSession(): Flow<Session?> {
    return userRepository.observeCurrentUser()
      .flatMapLatest { user ->
        db.sessionQueries
          .getActive(user.id)
          .asFlow()
          .mapToOneOrNull(dispatcherProvider.databaseRead)
          .map {
            it?.let { model -> hydrateSession(model) }
          }
      }
  }

  override suspend fun getSession(libraryItemId: LibraryItemId): Session? {
    val currentUser = userRepository.getCurrentUser()
    return withContext(dispatcherProvider.databaseRead) {
      db.sessionQueries.getForId(libraryItemId, currentUser.id)
        .executeAsOneOrNull()
        ?.let { hydrateSession(it) }
    }
  }

  override suspend fun createOrStartSession(
    libraryItemId: LibraryItemId,
    playMethod: PlayMethod,
    mediaPlayer: String,
    duration: Duration,
    currentTime: Duration,
    startedAt: LocalDateTime,
  ): Session {
    val currentUser = userRepository.getCurrentUser()
    return withContext(dispatcherProvider.databaseRead) {
      val existingSession = db.sessionQueries.getForId(
        libraryItemId = libraryItemId,
        userId = currentUser.id,
      ).executeAsOneOrNull()
      if (existingSession != null) {
        withContext(dispatcherProvider.databaseWrite) {
          db.transaction {
            db.sessionQueries.disableAll(currentUser.id)
            db.sessionQueries.enable(libraryItemId, currentUser.id)
          }
        }
        hydrateSession(existingSession)
      } else {
        withContext(dispatcherProvider.databaseWrite) {
          db.transaction {
            db.sessionQueries.disableAll(currentUser.id)
            db.sessionQueries.insert(
              DbSession(
                id = Uuid.random(),
                userId = currentUser.id,
                libraryItemId = libraryItemId,
                isActive = true,
                playMethod = PlayMethod.DirectPlay,
                mediaPlayer = "campfire",
                timeListening = 0.seconds,
                currentTime = currentTime,
                startedAt = fatherTime.now(),
                updatedAt = fatherTime.now(),
              ),
            )
          }
        }
        db.sessionQueries.getForId(libraryItemId, currentUser.id)
          .executeAsOne()
          .let { hydrateSession(it) }
      }
    }
  }

  override suspend fun updateCurrentTime(libraryItemId: LibraryItemId, currentTime: Duration) {
    val currentUser = userRepository.getCurrentUser()
    withContext(dispatcherProvider.databaseWrite) {
      // Update the playback session information with the new time
      db.sessionQueries.updatePlayback(
        libraryItemId = libraryItemId,
        userId = currentUser.id,
        currentTime = currentTime,
        updatedAt = fatherTime.now(),
      )
    }
  }

  override suspend fun addTimeListening(libraryItemId: LibraryItemId, amount: Duration) {
    val currentUser = userRepository.getCurrentUser()
    withContext(dispatcherProvider.databaseWrite) {
      db.sessionQueries.addTimeListening(
        libraryItemId = libraryItemId,
        userId = currentUser.id,
        timeListening = amount,
        updatedAt = fatherTime.now(),
      )
    }
  }

  override suspend fun deleteSession(libraryItemId: LibraryItemId) {
    val currentUser = userRepository.getCurrentUser()
    withContext(dispatcherProvider.databaseWrite) {
      db.sessionQueries.delete(libraryItemId, currentUser.id)
    }
  }

  override suspend fun stopSession(libraryItemId: LibraryItemId) {
    val currentUser = userRepository.getCurrentUser()
    withContext(dispatcherProvider.databaseWrite) {
      db.sessionQueries.disable(libraryItemId, currentUser.id)
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
      currentTime = session.currentTime,
      startedAt = session.startedAt,
      updatedAt = session.updatedAt,
    )
  }
}

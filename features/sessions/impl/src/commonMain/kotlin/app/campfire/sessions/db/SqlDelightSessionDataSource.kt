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
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
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
  private val libraryItemRepository: LibraryItemRepository,
  private val dispatcherProvider: DispatcherProvider,
) : SessionDataSource {

  override fun observeCurrentSession(): Flow<Session?> {
    return db.sessionQueries
      .getActive()
      .asFlow()
      .mapToOneOrNull(dispatcherProvider.databaseRead)
      .map {
        it?.let { model -> hydrateSession(model) }
      }
  }

  override suspend fun getSession(libraryItemId: LibraryItemId): Session? {
    return withContext(dispatcherProvider.databaseRead) {
      db.sessionQueries.getForId(libraryItemId)
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
    return withContext(dispatcherProvider.databaseRead) {
      val existingSession = db.sessionQueries.getForId(libraryItemId).executeAsOneOrNull()
      if (existingSession != null) {
        withContext(dispatcherProvider.databaseWrite) {
          db.transaction {
            db.sessionQueries.disableAll()
            db.sessionQueries.enable(libraryItemId)
          }
        }
        hydrateSession(existingSession)
      } else {
        withContext(dispatcherProvider.databaseWrite) {
          db.transaction {
            db.sessionQueries.disableAll()
            db.sessionQueries.insert(
              DbSession(
                id = Uuid.random(),
                libraryItemId = libraryItemId,
                isActive = true,
                playMethod = PlayMethod.DirectPlay,
                mediaPlayer = "campfire",
                duration = duration,
                timeListening = 0.seconds,
                startTime = 0.seconds,
                currentTime = currentTime,
                startedAt = fatherTime.now(),
                updatedAt = fatherTime.now(),
              ),
            )
          }
        }
        db.sessionQueries.getForId(libraryItemId)
          .executeAsOne()
          .let { hydrateSession(it) }
      }
    }
  }

  override suspend fun updateSession(libraryItemId: LibraryItemId, currentTime: Duration) {
    withContext(dispatcherProvider.databaseWrite) {
      db.transaction {
        // Update the playback session information with the new time
        db.sessionQueries.updatePlayback(
          libraryItemId = libraryItemId,
          currentTime = currentTime,
        )

        // Update the UserMediaProgress with the new time
        db.mediaProgressQueries.updateCurrentTime(
          currentTime = currentTime.toDouble(DurationUnit.SECONDS),
          lastUpdate = fatherTime.nowInEpochMillis(),
          libraryItemId = libraryItemId,
        )
      }
    }
  }

  override suspend fun deleteSession(libraryItemId: LibraryItemId) {
    withContext(dispatcherProvider.databaseWrite) {
      db.sessionQueries.delete(libraryItemId)
    }
  }

  override suspend fun stopSession(libraryItemId: LibraryItemId) {
    withContext(dispatcherProvider.databaseWrite) {
      db.sessionQueries.disable(libraryItemId)
    }
  }

  private suspend fun hydrateSession(session: DbSession): Session {
    val libraryItem = libraryItemRepository.getLibraryItem(session.libraryItemId)
    return Session(
      id = session.id,
      libraryItem = libraryItem,
      playMethod = session.playMethod,
      mediaPlayer = session.mediaPlayer,
      duration = session.duration,
      timeListening = session.timeListening,
      startTime = session.startTime,
      currentTime = session.currentTime,
      startedAt = session.startedAt,
      updatedAt = session.updatedAt,
    )
  }
}

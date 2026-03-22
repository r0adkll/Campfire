package app.campfire.audioplayer.impl.history

import app.campfire.CampfireDatabase
import app.campfire.audioplayer.history.PlaybackAction
import app.campfire.audioplayer.history.PlaybackHistoryRepository
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlaybackActionType
import app.campfire.core.session.UserSession
import app.campfire.core.session.userId
import app.campfire.core.time.FatherTime
import app.campfire.data.PlaybackAction as DbPlaybackAction
import app.campfire.settings.api.PlaybackSettings
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

@ContributesBinding(UserScope::class)
@Inject
class SqlDelightPlaybackHistoryRepository(
  private val userSession: UserSession,
  private val database: CampfireDatabase,
  private val fatherTime: FatherTime,
  private val playbackSettings: PlaybackSettings,
  private val dispatcherProvider: DispatcherProvider,
) : PlaybackHistoryRepository {

  override suspend fun record(
    libraryItemId: LibraryItemId,
    type: PlaybackActionType,
    fromPosition: Duration,
    toPosition: Duration?,
  ) {
    if (!playbackSettings.playbackHistoryEnabled) return
    val userId = userSession.userId ?: return
    write {
      database.playbackActionQueries.insert(
        libraryItemId = libraryItemId,
        userId = userId,
        type = type,
        timestamp = fatherTime.now(),
        fromPosition = fromPosition,
        toPosition = toPosition,
      )
    }
  }

  override fun observe(libraryItemId: LibraryItemId): Flow<List<PlaybackAction>> {
    val userId = userSession.userId ?: return emptyFlow()
    return database.playbackActionQueries
      .getForLibraryItem(libraryItemId, userId)
      .asFlow()
      .mapToList(dispatcherProvider.databaseRead)
      .map { list -> list.map { it.toDomain() } }
  }

  override suspend fun get(libraryItemId: LibraryItemId): List<PlaybackAction> {
    val userId = userSession.userId ?: return emptyList()
    return read {
      database.playbackActionQueries
        .getForLibraryItem(libraryItemId, userId)
        .awaitAsList()
        .map { it.toDomain() }
    }
  }

  override suspend fun clear(libraryItemId: LibraryItemId) {
    val userId = userSession.userId ?: return
    write {
      database.playbackActionQueries
        .deleteForLibraryItem(
          libraryItemId = libraryItemId,
          userId = userId,
        )
    }
  }

  override suspend fun clearAll() {
    val userId = userSession.userId ?: return
    write {
      database.playbackActionQueries.deleteAll(userId)
    }
  }

  private fun DbPlaybackAction.toDomain(): PlaybackAction = PlaybackAction(
    id = id,
    libraryItemId = libraryItemId,
    userId = userId,
    type = type,
    timestamp = timestamp,
    fromPosition = fromPosition,
    toPosition = toPosition,
  )

  private suspend fun <T> read(block: suspend CoroutineScope.() -> T) = withContext(
    context = dispatcherProvider.databaseRead,
    block = block,
  )

  private suspend fun <T> write(block: suspend CoroutineScope.() -> T) = withContext(
    context = dispatcherProvider.databaseWrite,
    block = block,
  )
}

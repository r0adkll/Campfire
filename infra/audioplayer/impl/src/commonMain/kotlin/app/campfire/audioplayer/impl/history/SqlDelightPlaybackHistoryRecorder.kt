package app.campfire.audioplayer.impl.history

import app.campfire.CampfireDatabase
import app.campfire.audioplayer.history.PlaybackAction
import app.campfire.audioplayer.history.PlaybackHistoryRecorder
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import app.campfire.core.session.UserSession
import app.campfire.core.session.requiredUserId
import app.campfire.data.PlaybackAction as DbPlaybackAction
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.r0adkll.kimchi.annotations.ContributesBinding
import com.r0adkll.kimchi.annotations.ContributesTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

@ContributesTo(UserScope::class)
interface PlaybackHistoryComponent {
  val playbackHistoryUserSession: UserSession
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class SqlDelightPlaybackHistoryRecorder(
  private val database: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
) : PlaybackHistoryRecorder {

  private val component: PlaybackHistoryComponent
    get() = ComponentHolder.component<PlaybackHistoryComponent>()

  private val userId: String
    get() = component.playbackHistoryUserSession.requiredUserId

  override suspend fun record(action: PlaybackAction) {
    write {
      database.playbackActionQueries.insert(
        libraryItemId = action.libraryItemId,
        userId = action.userId,
        type = action.type,
        timestamp = action.timestamp,
        fromPosition = action.fromPosition,
        toPosition = action.toPosition,
      )
    }
  }

  override fun observeActions(libraryItemId: LibraryItemId): Flow<List<PlaybackAction>> {
    return database.playbackActionQueries
      .getForLibraryItem(libraryItemId, userId)
      .asFlow()
      .mapToList(dispatcherProvider.databaseRead)
      .map { list -> list.map { it.toDomain() } }
  }

  override suspend fun getActions(libraryItemId: LibraryItemId): List<PlaybackAction> {
    return read {
      database.playbackActionQueries
        .getForLibraryItem(libraryItemId, userId)
        .awaitAsList()
        .map { it.toDomain() }
    }
  }

  override suspend fun clearActions(libraryItemId: LibraryItemId) {
    write {
      database.playbackActionQueries
        .deleteForLibraryItem(
          libraryItemId = libraryItemId,
          userId = userId,
        )
    }
  }

  override suspend fun clearAll() {
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

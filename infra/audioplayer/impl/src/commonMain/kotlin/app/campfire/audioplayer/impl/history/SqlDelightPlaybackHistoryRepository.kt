package app.campfire.audioplayer.impl.history

import app.campfire.CampfireDatabase
import app.campfire.audioplayer.history.PlaybackAction
import app.campfire.audioplayer.history.PlaybackHistoryRepository
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.core.session.UserSession
import app.campfire.core.session.userId
import app.campfire.data.PlaybackAction as DbPlaybackAction
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.r0adkll.kimchi.annotations.ContributesBinding
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
  private val dispatcherProvider: DispatcherProvider,
) : PlaybackHistoryRepository {

  override fun observe(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
  ): Flow<List<PlaybackAction>> {
    val userId = userSession.userId ?: return emptyFlow()
    val query = if (episodeId != null) {
      database.playbackActionQueries.getForEpisode(libraryItemId, userId, episodeId)
    } else {
      database.playbackActionQueries.getForLibraryItem(libraryItemId, userId)
    }
    return query
      .asFlow()
      .mapToList(dispatcherProvider.databaseRead)
      .map { list -> list.map { it.toDomain() } }
  }

  override suspend fun get(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
  ): List<PlaybackAction> {
    val userId = userSession.userId ?: return emptyList()
    return read {
      val query = if (episodeId != null) {
        database.playbackActionQueries.getForEpisode(libraryItemId, userId, episodeId)
      } else {
        database.playbackActionQueries.getForLibraryItem(libraryItemId, userId)
      }
      query
        .awaitAsList()
        .map { it.toDomain() }
    }
  }

  override suspend fun clear(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
  ) {
    val userId = userSession.userId ?: return
    write {
      if (episodeId != null) {
        database.playbackActionQueries.deleteForEpisode(
          libraryItemId = libraryItemId,
          userId = userId,
          episodeId = episodeId,
        )
      } else {
        database.playbackActionQueries.deleteForLibraryItem(
          libraryItemId = libraryItemId,
          userId = userId,
        )
      }
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
    // '' is the DB sentinel for "no episode" (book progress) — surface null in the domain.
    episodeId = episodeId.takeIf { it.isNotEmpty() },
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

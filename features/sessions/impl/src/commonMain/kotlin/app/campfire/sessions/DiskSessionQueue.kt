package app.campfire.sessions

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.session.UserSession
import app.campfire.core.session.requiredUserId
import app.campfire.data.SessionQueue as DbSessionQueue
import app.campfire.data.mapping.dao.LibraryItemDao
import app.campfire.data.mapping.model.mapToLibraryItemWithProgress
import app.campfire.sessions.api.SessionQueue
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

@ContributesBinding(UserScope::class)
@Inject
class DiskSessionQueue(
  private val userSession: UserSession,
  private val db: CampfireDatabase,
  private val libraryItemDao: LibraryItemDao,
  private val dispatcherProvider: DispatcherProvider,
) : SessionQueue {

  override suspend fun add(libraryItem: LibraryItem) {
    val newIndex = read {
      db.sessionQueueQueries
        .getHighestIndex()
        .awaitAsOneOrNull()
        ?.plus(1)
        ?: 0
    }

    write {
      db.sessionQueueQueries.insert(
        DbSessionQueue(
          userId = userSession.requiredUserId,
          libraryItemId = libraryItem.id,
          queueIndex = newIndex,
        ),
      )
    }
  }

  override suspend fun remove(libraryItem: LibraryItem) {
    val queue = read {
      db.sessionQueueQueries
        .selectAll(userSession.requiredUserId)
        .awaitAsList()
        .sortedBy { it.queueIndex }
        .map { it.libraryItemId }
        .filter { it != libraryItem.id }
    }

    db.sessionQueueQueries.transaction {
      db.sessionQueueQueries.delete(
        userId = userSession.requiredUserId,
        libraryItemId = libraryItem.id,
      )

      // Now re-index queue
      reindexQueue(queue)
    }
  }

  override suspend fun pop(): LibraryItem? {
    val queue = read {
      db.sessionQueueQueries
        .selectAll(userSession.requiredUserId)
        .awaitAsList()
    }

    val first = queue.firstOrNull()

    if (first != null) {
      write {
        db.sessionQueueQueries.delete(
          userId = userSession.requiredUserId,
          libraryItemId = first.libraryItemId,
        )

        val remaining = queue.drop(1)
        if (remaining.isNotEmpty()) {
          reindexQueue(remaining.map { it.libraryItemId })
        }
      }

      val libraryItem = read {
        db.libraryItemsQueries
          .selectForIdFull(
            first.libraryItemId,
            ::mapToLibraryItemWithProgress,
          )
          .awaitAsOneOrNull()
      }

      return libraryItem?.let {
        libraryItemDao.hydrateItem(it)
      }
    }

    return null
  }

  override suspend fun reorder(fromItemId: LibraryItemId, toItemId: LibraryItemId) {
    val queue = read {
      db.sessionQueueQueries
        .selectAll(userSession.requiredUserId)
        .awaitAsList()
    }

    val fromIndex = queue.indexOfFirst { it.libraryItemId == fromItemId }
    val toIndex = queue.indexOfFirst { it.libraryItemId == toItemId }

    val mutableQueue = queue.toMutableList()
    mutableQueue.add(toIndex, mutableQueue.removeAt(fromIndex))

    write {
      db.sessionQueueQueries.transaction {
        reindexQueue(mutableQueue.map { it.libraryItemId })
      }
    }
  }

  override suspend fun clear() {
    write {
      db.sessionQueueQueries.deleteAll(userSession.requiredUserId)
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeAll(): Flow<List<LibraryItem>> {
    return db.sessionQueueQueries
      .selectForUser(userSession.requiredUserId, ::mapToLibraryItemWithProgress)
      .asFlow()
      .mapToList(dispatcherProvider.databaseRead)
      .mapLatest { entities ->
        entities.map {
          libraryItemDao.hydrateItem(it)
        }
      }
  }

  private suspend fun reindexQueue(
    queue: List<LibraryItemId>,
  ) {
    queue.forEachIndexed { index, item ->
      db.sessionQueueQueries.updateIndex(
        queueIndex = index,
        userId = userSession.requiredUserId,
        libraryItemId = item,
      )
    }
  }

  private suspend inline fun <T> write(
    noinline block: suspend CoroutineScope.() -> T,
  ): T = withContext(
    dispatcherProvider.databaseWrite,
    block = block,
  )

  private suspend inline fun <T> read(
    noinline block: suspend CoroutineScope.() -> T,
  ): T = withContext(
    dispatcherProvider.databaseWrite,
    block = block,
  )
}

// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.core.session.UserSession
import app.campfire.core.session.requiredUserId
import app.campfire.core.session.userId
import app.campfire.data.SessionQueue as DbSessionQueue
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.dao.LibraryItemDao
import app.campfire.sessions.api.QueuedEntry
import app.campfire.sessions.api.SessionQueue
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext

@ContributesBinding(UserScope::class)
@Inject
class DiskSessionQueue(
  private val userSession: UserSession,
  private val db: CampfireDatabase,
  private val libraryItemDao: LibraryItemDao,
  private val dispatcherProvider: DispatcherProvider,
) : SessionQueue {

  override suspend fun add(libraryItemId: LibraryItemId, episodeId: PodcastEpisodeId?) {
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
          libraryItemId = libraryItemId,
          episodeId = episodeId.orEmpty(),
          queueIndex = newIndex,
        ),
      )
    }
  }

  override suspend fun addAll(libraryItems: List<LibraryItem>) {
    val newIndex = read {
      db.sessionQueueQueries
        .getHighestIndex()
        .awaitAsOneOrNull()
        ?.plus(1)
        ?: 0
    }

    write {
      db.sessionQueueQueries.transaction {
        libraryItems.forEachIndexed { index, item ->
          db.sessionQueueQueries.insert(
            DbSessionQueue(
              userId = userSession.requiredUserId,
              libraryItemId = item.id,
              episodeId = "",
              queueIndex = newIndex + index,
            ),
          )
        }
      }
    }
  }

  override suspend fun addFirst(libraryItem: LibraryItem, episode: PodcastEpisode?) {
    insert(libraryItem.id, episode?.id, index = 0)
  }

  override suspend fun insert(libraryItemId: LibraryItemId, episodeId: PodcastEpisodeId?, index: Int) {
    val entry = QueueKey(libraryItemId, episodeId.orEmpty())
    write {
      db.sessionQueueQueries.transaction {
        val rest = db.sessionQueueQueries
          .selectAll(userSession.requiredUserId)
          .awaitAsList()
          .map { QueueKey(it.libraryItemId, it.episodeId) }
          .filterNot { it == entry }
        val position = index.coerceIn(0, rest.size)

        db.sessionQueueQueries.delete(
          userId = userSession.requiredUserId,
          libraryItemId = entry.libraryItemId,
          episodeId = entry.episodeId,
        )
        db.sessionQueueQueries.insert(
          DbSessionQueue(
            userId = userSession.requiredUserId,
            libraryItemId = entry.libraryItemId,
            episodeId = entry.episodeId,
            queueIndex = position,
          ),
        )
        reindexQueue(rest.take(position) + entry + rest.drop(position))
      }
    }
  }

  override suspend fun remove(libraryItemId: LibraryItemId, episodeId: PodcastEpisodeId?): Int? {
    val target = QueueKey(libraryItemId, episodeId.orEmpty())
    val queue = read {
      db.sessionQueueQueries
        .selectAll(userSession.requiredUserId)
        .awaitAsList()
        .sortedBy { it.queueIndex }
        .map { QueueKey(it.libraryItemId, it.episodeId) }
    }
    val index = queue.indexOf(target).takeIf { it >= 0 }

    var removed = false
    db.sessionQueueQueries.transaction {
      removed = db.sessionQueueQueries.delete(
        userId = userSession.requiredUserId,
        libraryItemId = target.libraryItemId,
        episodeId = target.episodeId,
      ) > 0

      if (removed) reindexQueue(queue - target)
    }
    return index.takeIf { removed }
  }

  override suspend fun pop(): QueuedEntry? {
    val queue = read {
      db.sessionQueueQueries
        .selectAll(userSession.requiredUserId)
        .awaitAsList()
        .sortedBy { it.queueIndex }
    }

    val first = queue.firstOrNull() ?: return null

    write {
      db.sessionQueueQueries.delete(
        userId = userSession.requiredUserId,
        libraryItemId = first.libraryItemId,
        episodeId = first.episodeId,
      )

      val remaining = queue.drop(1)
      if (remaining.isNotEmpty()) {
        reindexQueue(remaining.map { QueueKey(it.libraryItemId, it.episodeId) })
      }
    }

    return hydrate(first.libraryItemId, first.episodeId.takeIf { it.isNotEmpty() })
  }

  override suspend fun reorder(entries: List<QueuedEntry>) {
    val queue = read {
      db.sessionQueueQueries
        .selectAll(userSession.requiredUserId)
        .awaitAsList()
    }

    if (queue.size != entries.size) return

    write {
      db.sessionQueueQueries.transaction {
        reindexQueue(entries.map { QueueKey(it.libraryItemId, it.episodeId.orEmpty()) })
      }
    }
  }

  override suspend fun clear() {
    write {
      db.sessionQueueQueries.deleteAll(userSession.requiredUserId)
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeAll(): Flow<List<QueuedEntry>> {
    if (userSession.userId == null) return emptyFlow()
    return db.sessionQueueQueries
      .selectAll(userSession.requiredUserId)
      .asFlow()
      .mapToList(dispatcherProvider.databaseRead)
      .mapLatest { rows ->
        rows
          .sortedBy { it.queueIndex }
          .mapNotNull { row ->
            hydrate(row.libraryItemId, row.episodeId.takeIf { it.isNotEmpty() })
          }
      }
  }

  private suspend fun hydrate(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
  ): QueuedEntry? {
    val libraryItem = libraryItemDao.hydrateById(libraryItemId) ?: return null
    val episode = episodeId?.let {
      read {
        db.podcastEpisodeQueries.selectForId(it).awaitAsOneOrNull()
      }?.asDomainModel()
    }
    return QueuedEntry(libraryItem = libraryItem, episode = episode)
  }

  private suspend fun reindexQueue(queue: List<QueueKey>) {
    queue.forEachIndexed { index, key ->
      db.sessionQueueQueries.updateIndex(
        queueIndex = index,
        userId = userSession.requiredUserId,
        libraryItemId = key.libraryItemId,
        episodeId = key.episodeId,
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

  private data class QueueKey(
    val libraryItemId: String,
    val episodeId: String,
  )
}

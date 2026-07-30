// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.user.mediaprogress.store

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.core.model.UserId
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDomainModel
import app.campfire.user.mediaprogress.store.MediaProgressStore.Operation
import app.campfire.user.mediaprogress.store.MediaProgressStore.Output
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.mobilenativefoundation.store.store5.SourceOfTruth

class MediaProgressSourceOfTruthFactory(
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
) {

  fun create(): SourceOfTruth<Operation, Output, Output> = SourceOfTruth.of(
    reader = { operation ->
      when (operation) {
        is Operation.Query.All -> observeAll(operation.userId)
        is Operation.Query.One -> observeByLibraryItemId(
          userId = operation.userId,
          libraryItemId = operation.libraryItemId,
          episodeId = operation.episodeId,
        )
      }
    },
    writer = { operation, output ->
      handleWrite(operation, output)
    },
    delete = { operation ->
      require(operation is Operation.Query.One)
      handleDelete(operation)
    },
  )

  private fun observeAll(userId: UserId): Flow<Output.Collection> {
    return db.mediaProgressQueries
      .selectForUser(userId)
      .asFlow()
      .mapToList(dispatcherProvider.databaseRead)
      .map { it.map { p -> p.asDomainModel() } }
      .map { Output.Collection(it) }
  }

  private fun observeByLibraryItemId(
    userId: UserId,
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
  ): Flow<Output.Single> {
    val query = if (episodeId != null) {
      db.mediaProgressQueries.selectForEpisode(
        userId = userId,
        libraryItemId = libraryItemId,
        episodeId = episodeId,
      )
    } else {
      db.mediaProgressQueries.selectForLibraryItem(userId, libraryItemId)
    }
    return query
      .asFlow()
      .mapToOneOrNull(dispatcherProvider.databaseRead)
      .map { Output.Single(it?.asDomainModel()) }
  }

  private suspend fun handleWrite(operation: Operation, output: Output = Output.Collection(emptyList())) {
    when (operation) {
      is Operation.Query.All -> writeOutput(output)
      is Operation.Query.One -> writeOutput(output)
    }
  }

  private suspend fun handleDelete(operation: Operation.Query, output: Output = Output.Collection(emptyList())) {
    when (operation) {
      is Operation.Query.All -> deleteAll(operation.userId)
      is Operation.Query.One -> deleteSingle(
        userId = operation.userId,
        libraryItemId = operation.libraryItemId,
        episodeId = operation.episodeId,
      )
    }
  }

  private suspend fun writeOutput(output: Output) {
    when (output) {
      is Output.Collection -> writeAll(output)
      is Output.Single -> writeSingle(output)
    }
  }

  private suspend fun writeAll(output: Output.Collection) = withContext(dispatcherProvider.databaseWrite) {
    db.mediaProgressQueries.transaction {
      output.items.forEach { item ->
        insertIfFresher(item)
      }
    }
  }

  private suspend fun writeSingle(output: Output.Single) = withContext(dispatcherProvider.databaseWrite) {
    output.item?.let { item ->
      // Atomic select+insert: prevents a stale fetch from clobbering a fresher local row
      // that a concurrent playback tick wrote between the select and the insert.
      db.mediaProgressQueries.transaction {
        insertIfFresher(item)
      }
    }
  }

  /**
   * Insert [item] only when the existing DB row is missing or has an equal-or-older
   * `lastUpdate`. The Store5 fetcher fires on every `StoreReadRequest.cached(refresh = true)`
   * — including the one the playback presenter triggers on every player-bar
   * expand/collapse to refresh the sync banner. During continuous playback the local row
   * is updated every player tick but the local→server PATCH is throttled, so a refetch
   * during that window returns a stale-but-`Source.Remote` row. Without this guard the
   * stale row would clobber the fresher local one and oscillate the displayed progress
   * back to 0 (or whatever the server last saw).
   *
   * Mirrors the conflict-resolution shape used by [app.campfire.data.mapping.dao.LibraryItemDao.insertPodcast]
   * for its user progress sub-row write.
   */
  private suspend fun insertIfFresher(item: MediaProgress) {
    val existing = db.mediaProgressQueries.selectForEpisode(
      userId = item.userId,
      libraryItemId = item.libraryItemId,
      // DB sentinel for "no episode" (book progress) is ''; episodes carry their id.
      episodeId = item.episodeId.orEmpty(),
    ).awaitAsOneOrNull()
    if (existing == null || existing.lastUpdate <= item.lastUpdate) {
      db.mediaProgressQueries.insert(item.asDbModel())
    }
  }

  private suspend fun deleteSingle(
    userId: UserId,
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
  ) {
    withContext(dispatcherProvider.databaseWrite) {
      db.mediaProgressQueries.deleteForEpisode(
        userId = userId,
        libraryItemId = libraryItemId,
        episodeId = episodeId.orEmpty(),
      )
    }
  }

  private suspend fun deleteAll(userId: UserId) {
    withContext(dispatcherProvider.databaseWrite) {
      db.mediaProgressQueries.deleteForUser(userId)
    }
  }
}

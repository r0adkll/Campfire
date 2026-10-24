// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.user

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.core.session.UserSession
import app.campfire.core.session.requiredUserId
import app.campfire.core.session.userId
import app.campfire.core.time.FatherTime
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.dao.LibraryItemDao
import app.campfire.data.mapping.store.debugLogging
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.envelopes.MediaProgressUpdatePayload
import app.campfire.network.models.MediaProgress as NetworkMediaProgress
import app.campfire.user.api.MediaProgressKey
import app.campfire.user.api.MediaProgressRepository
import app.campfire.user.api.StartedAtNotChanged
import app.campfire.user.mediaprogress.EbookProgressUploader
import app.campfire.user.mediaprogress.MediaProgressSynchronizer
import app.campfire.user.mediaprogress.store.MediaProgressStore
import app.campfire.user.mediaprogress.store.MediaProgressStore.Operation
import app.campfire.user.mediaprogress.store.MediaProgressStore.Output
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.mobilenativefoundation.store.store5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.impl.extensions.fresh
import org.mobilenativefoundation.store.store5.impl.extensions.get

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStoreApi::class)
@ContributesBinding(UserScope::class)
@SingleIn(UserScope::class)
@Inject
class StoreMediaProgressRepository(
  private val userSession: UserSession,
  private val storeFactory: MediaProgressStore.Factory,
  private val db: CampfireDatabase,
  private val api: AudioBookShelfApi,
  private val libraryItemDao: LibraryItemDao,
  private val mediaProgressSynchronizer: MediaProgressSynchronizer,
  private val ebookProgressUploader: EbookProgressUploader,
  private val fatherTime: FatherTime,
  private val dispatcherProvider: DispatcherProvider,
) : MediaProgressRepository {

  private val store: Store<Operation, Output> by lazy { storeFactory.create() }

  override fun observeProgress(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
    refresh: Boolean,
  ): Flow<MediaProgress?> {
    val request = StoreReadRequest.cached(
      key = Operation.Query.One(userSession.requiredUserId, libraryItemId, episodeId),
      refresh = refresh,
    )
    return store.stream(request)
      .debugLogging(
        tag = "MediaProgressStore::observeProgress($libraryItemId, $episodeId)",
        enabled = MediaProgressStore.enabled,
      )
      .map { it.dataOrNull() }
      .filterNotNull()
      .map { it.requireSingle() }
  }

  override suspend fun getProgress(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
    fresh: Boolean,
  ): MediaProgress? {
    val userId = userSession.userId ?: return null
    val operation = Operation.Query.One(userId, libraryItemId, episodeId)
    return try {
      if (fresh) {
        store.fresh(operation).requireSingle()
      } else {
        store.get(operation).requireSingle()
      }
    } catch (_: Exception) {
      null
    }
  }

  override fun observeAllProgress(): Flow<List<MediaProgress>> {
    val userId = userSession.userId ?: return emptyFlow()
    val request = StoreReadRequest.cached(Operation.Query.All(userId), false)
    return store.stream(request)
      .debugLogging(
        tag = "MediaProgressStore::observeAllProgress",
        enabled = MediaProgressStore.enabled,
      )
      .map { it.dataOrNull() }
      .filterNotNull()
      .map { it.requireCollection() }
  }

  override suspend fun updateProgress(
    newProgress: MediaProgress,
    force: Boolean,
    skipUpload: Boolean,
    onlyIfFresher: Boolean,
  ) {
    // Update local storage
    val progressId = db.mediaProgressQueries.transactionWithResult<String?> {
      val existing = db.mediaProgressQueries.selectForEpisode(
        userId = newProgress.userId,
        libraryItemId = newProgress.libraryItemId,
        // DB stores book progress under the empty-string sentinel; episodes carry their id.
        episodeId = newProgress.episodeId.orEmpty(),
      ).awaitAsOneOrNull()

      if (onlyIfFresher && existing != null && existing.lastUpdate > newProgress.lastUpdate) {
        // The stored row is fresher (e.g. this device's own live playback writes) —
        // dropping the incoming stale echo is the same last-write-wins rule the fetch
        // path's insertIfFresher applies.
        return@transactionWithResult null
      }

      db.mediaProgressQueries.insert(
        newProgress.asDbModel(existing?.id),
      )

      existing?.id?.takeIf { it != MediaProgress.UNKNOWN_ID } ?: newProgress.id
    } ?: return

    val updatedProgress = newProgress.copy(id = progressId)

    // Kick off potential synchronizer (suppressed when the write originated remotely).
    if (!skipUpload) {
      mediaProgressSynchronizer.sync(updatedProgress, force)
    }
  }

  override suspend fun updateEbookProgress(
    libraryItemId: LibraryItemId,
    location: String,
    progress: Float,
  ) {
    val userId = userSession.requiredUserId
    val now = fatherTime.nowInEpochMillis()
    val existing = withContext(dispatcherProvider.databaseRead) {
      // Empty string is the DB sentinel for "no episode" (book-level progress)
      db.mediaProgressQueries.selectForEpisode(userId, libraryItemId, "").awaitAsOneOrNull()
    }

    val updated = existing?.copy(
      ebookLocation = location,
      ebookProgress = progress.toDouble(),
      lastUpdate = now,
      source = MediaProgress.Source.Local,
    ) ?: run {
      val libraryItem = libraryItemDao.hydrateById(libraryItemId) ?: run {
        MediaProgressStore.ebark { "Unable to find library item for $libraryItemId" }
        return
      }
      app.campfire.data.MediaProgress(
        // Server driven, so a placeholder until the upload fetches the record back
        id = MediaProgress.UNKNOWN_ID,
        userId = userId,
        libraryItemId = libraryItemId,
        episodeId = "",
        mediaItemId = libraryItem.media.id,
        mediaItemType = libraryItem.mediaType,
        duration = null,
        progress = 0.0,
        currentTime = 0.0,
        isFinished = false,
        hideFromContinueListening = false,
        ebookLocation = location,
        ebookProgress = progress.toDouble(),
        finishedAt = null,
        lastUpdate = now,
        startedAt = now,
        source = MediaProgress.Source.Local,
      )
    }

    withContext(dispatcherProvider.databaseWrite) {
      db.mediaProgressQueries.insert(updated)
    }
    ebookProgressUploader.request(libraryItemId)
  }

  override suspend fun deleteProgress(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
  ) {
    val currentUserId = userSession.requiredUserId
    val operation = Operation.Query.One(currentUserId, libraryItemId, episodeId)
    val existing = store.get(operation).requireSingle()
    if (existing != null && existing.id != MediaProgress.UNKNOWN_ID) {
      api.deleteMediaProgress(existing.id)
        .onSuccess {
          store.clear(operation)
        }
        .onFailure {
          MediaProgressStore.ebark(throwable = it) { "Failed to delete MediaProgress for $libraryItemId" }
        }
    }
  }

  override suspend fun markFinished(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
  ) {
    api.updateMediaProgress(
      libraryItemId = libraryItemId,
      episodeId = episodeId,
      update = MediaProgressUpdatePayload(
        episodeId = episodeId,
        isFinished = true,
        finishedAt = fatherTime.nowInEpochMillis(),
      ),
    ).onSuccess {
      val currentUserId = userSession.requiredUserId
      val operation = Operation.Query.One(currentUserId, libraryItemId, episodeId)
      val existing = store.get(operation).requireSingle()
      // Playback writes the local row before the server has issued its id, so a row still
      // holding the placeholder id is the same progress and is marked finished all the same.
      if (existing != null) {
        withContext(dispatcherProvider.databaseWrite) {
          db.mediaProgressQueries.markFinished(
            timestamp = fatherTime.nowInEpochMillis(),
            userId = currentUserId,
            libraryItemId = libraryItemId,
            // Empty string is the DB sentinel for "no episode" (book-level progress).
            episodeId = episodeId.orEmpty(),
          )
        }
      } else {
        // Use the dao so the lookup dispatches on mediaType — the book-only
        // selectForId joins `media`, which returns nothing for podcast items
        // (their row lives in `podcastMedia`).
        val libraryItem = libraryItemDao.hydrateById(libraryItemId) ?: run {
          MediaProgressStore.ebark { "Unable to find library item for $libraryItemId" }
          return@onSuccess
        }

        // For podcast episodes the per-episode duration lives on the podcastEpisode row;
        // Media.Podcast.durationInMillis is the sum across all episodes, which isn't what
        // we want to record on a single episode's progress row.
        val durationMillis = if (episodeId != null) {
          val episodeRow = withContext(dispatcherProvider.databaseRead) {
            db.podcastEpisodeQueries.selectForId(episodeId).awaitAsOneOrNull()
          }
          episodeRow?.durationInMillis ?: libraryItem.media.durationInMillis
        } else {
          libraryItem.media.durationInMillis
        }

        // If we don't have an existing media progress id, lets create one
        val newMediaProgress = app.campfire.data.MediaProgress(
          // This ID is server driven and there is no way to determine it without
          // syncing progress back from the server. So we instead use a placeholder
          // and update this later in the syncing logic.
          id = MediaProgress.UNKNOWN_ID,
          userId = userSession.requiredUserId,
          libraryItemId = libraryItemId,
          episodeId = episodeId.orEmpty(),
          mediaItemId = episodeId ?: libraryItem.media.id,
          mediaItemType = libraryItem.mediaType,
          duration = durationMillis.milliseconds.toDouble(DurationUnit.SECONDS),
          progress = 1.0,
          currentTime = 0.0,
          isFinished = true,
          hideFromContinueListening = true,
          ebookLocation = null,
          ebookProgress = null,
          finishedAt = fatherTime.nowInEpochMillis(),
          lastUpdate = fatherTime.nowInEpochMillis(),
          startedAt = fatherTime.nowInEpochMillis(),
          source = MediaProgress.Source.Local,
        )

        withContext(dispatcherProvider.databaseWrite) {
          db.mediaProgressQueries.insert(
            newMediaProgress,
          )
        }

        // This is heavy handed and duplicative as it will force another update request
        // followed by a fetch to hydrate its ACTUAL id.
        mediaProgressSynchronizer.sync(newMediaProgress.asDomainModel(), force = true)
      }
    }.onFailure {
      MediaProgressStore.ebark(throwable = it) { "Error marking finished for libraryItemId $libraryItemId" }
    }
  }

  override suspend fun markNotFinished(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
  ) {
    // First we just fetch the existing media progressId for the given (library item, episode)
    val mediaProgressId = db.mediaProgressQueries
      .getMediaProgressId(
        userId = userSession.requiredUserId,
        libraryItemId = libraryItemId,
        episodeId = episodeId.orEmpty(),
      )
      .awaitAsOneOrNull()

    // If it exists and is not an un-synced Id
    if (mediaProgressId != null && mediaProgressId != MediaProgress.UNKNOWN_ID) {
      api.deleteMediaProgress(mediaProgressId)
        .onSuccess {
          withContext(dispatcherProvider.databaseWrite) {
            deleteLocalProgress(libraryItemId, episodeId)
          }
        }
        .onFailure {
          MediaProgressStore.ebark(throwable = it) {
            "Error deleting progress for libraryItemId $libraryItemId"
          }
        }
    } else if (mediaProgressId == MediaProgress.UNKNOWN_ID) {
      // If we have an unsynced media progress, then lets fallback to the legacy
      // method and just use the update method to mark as not finished
      api.updateMediaProgress(
        libraryItemId = libraryItemId,
        episodeId = episodeId,
        update = MediaProgressUpdatePayload(
          episodeId = episodeId,
          isFinished = false,
          progress = 0f,
          currentTime = 0f,
          hideFromContinueListening = true,
        ),
      ).onSuccess {
        // Marking as "Not Finished" is effectively deleting it, so let's just remove
        // and let a future sync handle any updated sync.
        withContext(dispatcherProvider.databaseWrite) {
          deleteLocalProgress(libraryItemId, episodeId)
        }
      }.onFailure {
        MediaProgressStore.ebark(throwable = it) {
          "Error marking not finished for libraryItemId $libraryItemId"
        }
      }
    }
  }

  override suspend fun setFinishedAt(
    libraryItemId: LibraryItemId,
    finishedAt: Long,
    episodeId: PodcastEpisodeId?,
  ): Result<Unit> {
    val userId = userSession.requiredUserId
    val existing = withContext(dispatcherProvider.databaseRead) {
      // Empty string is the DB sentinel for "no episode" (book-level progress)
      db.mediaProgressQueries.selectForEpisode(userId, libraryItemId, episodeId.orEmpty()).awaitAsOneOrNull()
    }
    if (existing == null || !existing.isFinished) {
      return Result.failure(IllegalStateException("No finished progress for $libraryItemId to change"))
    }

    withContext(dispatcherProvider.databaseWrite) {
      db.mediaProgressQueries.insert(existing.copy(finishedAt = finishedAt))
    }

    // Only the finish fields: a position would let the server decide the item isn't finished
    return api.updateMediaProgress(
      libraryItemId = libraryItemId,
      episodeId = episodeId,
      update = MediaProgressUpdatePayload(
        episodeId = episodeId,
        isFinished = true,
        finishedAt = finishedAt,
      ),
    ).onFailure { error ->
      MediaProgressStore.ebark(throwable = error) { "Error changing the finish date of $libraryItemId" }
      withContext(dispatcherProvider.databaseWrite) {
        db.mediaProgressQueries.transaction {
          val current = db.mediaProgressQueries
            .selectForEpisode(userId, libraryItemId, episodeId.orEmpty())
            .awaitAsOneOrNull()
          // Put the old date back, unless something newer has replaced the row since
          if (current != null && current.finishedAt == finishedAt) {
            db.mediaProgressQueries.insert(current.copy(finishedAt = existing.finishedAt))
          }
        }
      }
    }
  }

  override suspend fun setStartedAt(
    libraryItemId: LibraryItemId,
    startedAt: Long,
    episodeId: PodcastEpisodeId?,
  ): Result<Unit> {
    // The server's copy, rather than this device's, is what gets created again
    val original = api.getMediaProgress(libraryItemId, episodeId)
      .getOrElse { return Result.failure(StartedAtNotChanged.Unchanged(it)) }
    api.deleteMediaProgress(original.id)
      .onFailure { return Result.failure(StartedAtNotChanged.Unchanged(it)) }

    api.updateMediaProgress(libraryItemId, original.asCreation(createdAt = startedAt), episodeId)
      .onFailure { error ->
        MediaProgressStore.ebark(throwable = error) { "Couldn't create the progress of $libraryItemId again" }
        val restored = createWithRetries(libraryItemId, original.asCreation(createdAt = original.startedAt), episodeId)
        return Result.failure(
          if (restored.isSuccess) {
            storeServerCopy(libraryItemId, episodeId)
            StartedAtNotChanged.Restored(error)
          } else {
            StartedAtNotChanged.NotRestored(error)
          },
        )
      }

    val recreated = storeServerCopy(libraryItemId, episodeId)
    if (recreated == null) {
      // Created, but not fetched back, so the new start stands in until the server's copy is
      withContext(dispatcherProvider.databaseWrite) {
        val userId = userSession.requiredUserId
        db.mediaProgressQueries.transaction {
          db.mediaProgressQueries
            .selectForEpisode(userId, libraryItemId, episodeId.orEmpty())
            .awaitAsOneOrNull()
            ?.let { db.mediaProgressQueries.insert(it.copy(id = MediaProgress.UNKNOWN_ID, startedAt = startedAt)) }
        }
      }
      return Result.success(Unit)
    }

    return if (recreated.startedAt == startedAt) {
      Result.success(Unit)
    } else {
      Result.failure(StartedAtNotChanged.ServerKept())
    }
  }

  override suspend fun restoreProgress(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
  ): Result<Unit> {
    val local = withContext(dispatcherProvider.databaseRead) {
      db.mediaProgressQueries
        .selectForEpisode(userSession.requiredUserId, libraryItemId, episodeId.orEmpty())
        .awaitAsOneOrNull()
    } ?: return Result.failure(IllegalStateException("No progress for $libraryItemId to restore"))

    return createWithRetries(libraryItemId, local.asDomainModel().asCreation(), episodeId)
      .onSuccess { storeServerCopy(libraryItemId, episodeId) }
  }

  /**
   * Create a progress on the server, trying again a couple of times, as the progress has already
   * been deleted from the server by the time this is needed.
   */
  private suspend fun createWithRetries(
    libraryItemId: LibraryItemId,
    creation: MediaProgressUpdatePayload,
    episodeId: PodcastEpisodeId?,
  ): Result<Unit> {
    var result = api.updateMediaProgress(libraryItemId, creation, episodeId)
    for (wait in RestoreRetryWaits) {
      if (result.isSuccess) break
      delay(wait)
      result = api.updateMediaProgress(libraryItemId, creation, episodeId)
    }
    return result.onFailure {
      MediaProgressStore.ebark(throwable = it) { "Couldn't put the progress of $libraryItemId back" }
    }
  }

  /** Fetch the server's copy of a progress into the database, which takes up its new id. */
  private suspend fun storeServerCopy(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
  ): NetworkMediaProgress? {
    val serverCopy = api.getMediaProgress(libraryItemId, episodeId).getOrNull() ?: return null
    withContext(dispatcherProvider.databaseWrite) {
      db.mediaProgressQueries.insert(serverCopy.asDbModel())
    }
    return serverCopy
  }

  override suspend fun markAllFinished(keys: List<MediaProgressKey>) {
    if (keys.isEmpty()) return
    val finishedAt = fatherTime.nowInEpochMillis()
    api.batchUpdateMediaProgress(
      keys.map { key ->
        MediaProgressUpdatePayload(
          libraryItemId = key.libraryItemId,
          episodeId = key.episodeId,
          isFinished = true,
          finishedAt = finishedAt,
        )
      },
    ).onSuccess {
      val currentUserId = userSession.requiredUserId
      val untracked = withContext(dispatcherProvider.databaseWrite) {
        db.mediaProgressQueries.transactionWithResult {
          keys.filter { key ->
            val tracked = db.mediaProgressQueries.getMediaProgressId(
              userId = currentUserId,
              libraryItemId = key.libraryItemId,
              episodeId = key.episodeId.orEmpty(),
            ).awaitAsOneOrNull() != null
            if (tracked) {
              db.mediaProgressQueries.markFinished(
                timestamp = finishedAt,
                userId = currentUserId,
                libraryItemId = key.libraryItemId,
                episodeId = key.episodeId.orEmpty(),
              )
            }
            !tracked
          }
        }
      }

      // The server created progress for the items that had none, so pull those rows down
      // rather than inventing local ones that would lack a server id.
      untracked.forEach { key ->
        try {
          store.fresh(Operation.Query.One(currentUserId, key.libraryItemId, key.episodeId))
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          MediaProgressStore.ebark(throwable = e) { "Failed to fetch new progress for ${key.libraryItemId}" }
        }
      }
    }.onFailure {
      MediaProgressStore.ebark(throwable = it) { "Error marking ${keys.size} items finished" }
    }
  }

  override suspend fun markAllNotFinished(keys: List<MediaProgressKey>) {
    // The server has no batch delete, and un-finishing through the batch update leaves a
    // reset row behind instead of removing the progress, so un-finish each item on its own.
    coroutineScope {
      keys
        .map { key -> async { markNotFinished(key.libraryItemId, key.episodeId) } }
        .awaitAll()
    }
  }

  /** Everything the server needs to create this progress again, starting at [createdAt]. */
  private fun NetworkMediaProgress.asCreation(createdAt: Long) = MediaProgressUpdatePayload(
    episodeId = episodeId,
    duration = duration,
    progress = progress,
    currentTime = currentTime,
    isFinished = isFinished,
    hideFromContinueListening = hideFromContinueListening,
    finishedAt = finishedAt,
    ebookLocation = ebookLocation,
    ebookProgress = ebookProgress,
    createdAt = createdAt,
  )

  private fun MediaProgress.asCreation() = MediaProgressUpdatePayload(
    episodeId = episodeId,
    duration = duration,
    progress = progress,
    currentTime = currentTime,
    isFinished = isFinished,
    hideFromContinueListening = hideFromContinueListening,
    finishedAt = finishedAt,
    ebookLocation = ebookLocation,
    ebookProgress = ebookProgress,
    createdAt = startedAt,
  )

  private suspend fun deleteLocalProgress(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
  ) {
    val currentUserId = userSession.requiredUserId
    db.mediaProgressQueries.deleteForEpisode(
      userId = currentUserId,
      libraryItemId = libraryItemId,
      episodeId = episodeId.orEmpty(),
    )
  }
}

/** How long to wait before each further try at putting a deleted progress back. */
private val RestoreRetryWaits = listOf(1.seconds, 3.seconds)

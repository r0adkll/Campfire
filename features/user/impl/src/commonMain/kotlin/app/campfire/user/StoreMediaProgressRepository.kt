package app.campfire.user

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
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
import app.campfire.user.api.MediaProgressRepository
import app.campfire.user.mediaprogress.MediaProgressSynchronizer
import app.campfire.user.mediaprogress.store.MediaProgressStore
import app.campfire.user.mediaprogress.store.MediaProgressStore.Operation
import app.campfire.user.mediaprogress.store.MediaProgressStore.Output
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
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
      .debugLogging("MediaProgressStore::observeProgress($libraryItemId, $episodeId)")
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
      .debugLogging("MediaProgressStore::observeAllProgress")
      .map { it.dataOrNull() }
      .filterNotNull()
      .map { it.requireCollection() }
  }

  override suspend fun updateProgress(
    newProgress: MediaProgress,
    force: Boolean,
    skipUpload: Boolean,
  ) {
    // Update local storage
    val progressId = db.mediaProgressQueries.transactionWithResult {
      val existing = db.mediaProgressQueries.selectForEpisode(
        userId = newProgress.userId,
        libraryItemId = newProgress.libraryItemId,
        // DB stores book progress under the empty-string sentinel; episodes carry their id.
        episodeId = newProgress.episodeId.orEmpty(),
      ).awaitAsOneOrNull()

      db.mediaProgressQueries.insert(
        newProgress.asDbModel(existing?.id),
      )

      existing?.id?.takeIf { it != MediaProgress.UNKNOWN_ID } ?: newProgress.id
    }

    val updatedProgress = newProgress.copy(id = progressId)

    // Kick off potential synchronizer (suppressed when the write originated remotely).
    if (!skipUpload) {
      mediaProgressSynchronizer.sync(updatedProgress, force)
    }
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
      if (existing != null && existing.id != MediaProgress.UNKNOWN_ID) {
        withContext(dispatcherProvider.databaseWrite) {
          db.mediaProgressQueries.markFinished(
            timestamp = fatherTime.nowInEpochMillis(),
            userId = currentUserId,
            libraryItemId = libraryItemId,
            // Empty string is the DB sentinel for "no episode" (book-level progress).
            episodeId = episodeId.orEmpty(),
          )
        }
      } else if (existing == null) {
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

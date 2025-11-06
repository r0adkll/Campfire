package app.campfire.user

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.session.UserSession
import app.campfire.core.session.requiredUserId
import app.campfire.core.session.userId
import app.campfire.core.time.FatherTime
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.model.mapToLibraryItem
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
  private val mediaProgressSynchronizer: MediaProgressSynchronizer,
  private val fatherTime: FatherTime,
  private val dispatcherProvider: DispatcherProvider,
) : MediaProgressRepository {

  private val store: Store<Operation, Output> by lazy { storeFactory.create() }

  override fun observeProgress(libraryItemId: LibraryItemId): Flow<MediaProgress?> {
    val request = StoreReadRequest.cached(Operation.Query.One(userSession.requiredUserId, libraryItemId), false)
    return store.stream(request)
      .debugLogging("MediaProgressStore::observeProgress")
      .map { it.dataOrNull() }
      .filterNotNull()
      .map { it.requireSingle() }
  }

  override suspend fun getProgress(libraryItemId: LibraryItemId): MediaProgress? {
    val userId = userSession.userId ?: return null
    val operation = Operation.Query.One(userId, libraryItemId)
    return store.get(operation).requireSingle()
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

  override suspend fun updateProgress(newProgress: MediaProgress, force: Boolean) {
    MediaProgressStore.ibark { "updateProgress <-- $newProgress" }

    // Update local storage
    val progressId = db.mediaProgressQueries.transactionWithResult {
      val existing = db.mediaProgressQueries.selectForLibraryItem(
        userId = newProgress.userId,
        libraryItemId = newProgress.libraryItemId,
      ).awaitAsOneOrNull()

      MediaProgressStore.vbark { "insertingProgress --> Existing($existing)" }

      db.mediaProgressQueries.insert(
        newProgress.asDbModel(existing?.id),
      )

      existing?.id?.takeIf { it != MediaProgress.UNKNOWN_ID } ?: newProgress.id
    }

    val updatedProgress = newProgress.copy(id = progressId)

    // Kick off potential synchronizer
    mediaProgressSynchronizer.sync(updatedProgress, force)
  }

  override suspend fun deleteProgress(libraryItemId: LibraryItemId) {
    val currentUserId = userSession.requiredUserId
    val existing = store.get(Operation.Query.One(currentUserId, libraryItemId))
      .requireSingle()
    if (existing != null && existing.id != MediaProgress.UNKNOWN_ID) {
      api.deleteMediaProgress(existing.id)
        .onSuccess {
          store.clear(Operation.Query.One(existing.userId, existing.libraryItemId))
          MediaProgressStore.ibark { "MediaProgress for $libraryItemId was deleted" }
        }
        .onFailure {
          MediaProgressStore.ebark { "MediaProgress for $libraryItemId failed to delete" }
        }
    } else {
      MediaProgressStore.ebark { "Error deleting progress for libraryItemId $libraryItemId" }
    }
  }

  override suspend fun markFinished(libraryItemId: LibraryItemId) {
    api.updateMediaProgress(
      libraryItemId = libraryItemId,
      update = MediaProgressUpdatePayload(
        isFinished = true,
        finishedAt = fatherTime.nowInEpochMillis(),
      ),
    ).onSuccess {
      val currentUserId = userSession.requiredUserId
      val existing = store.get(Operation.Query.One(currentUserId, libraryItemId))
        .requireSingle()
      if (existing != null && existing.id != MediaProgress.UNKNOWN_ID) {
        withContext(dispatcherProvider.databaseWrite) {
          db.mediaProgressQueries.markFinished(
            timestamp = fatherTime.nowInEpochMillis(),
            userId = currentUserId,
            libraryItemId = libraryItemId,
          )
        }
        MediaProgressStore.ibark { "MediaProgress[$libraryItemId] marked finished!" }
      } else if (existing == null) {
        val libraryItem = withContext(dispatcherProvider.databaseRead) {
          db.libraryItemsQueries
            .selectForId(libraryItemId, ::mapToLibraryItem)
            .awaitAsOneOrNull()
        } ?: run {
          MediaProgressStore.ebark { "Unable to find library item for $libraryItemId" }
          return@onSuccess
        }

        // If we don't have an existing media progress id, lets create one
        val newMediaProgress = app.campfire.data.MediaProgress(
          // This ID is server driven and there is no way to determine it without
          // syncing progress back from the server. So we instead use a placeholder
          // and update this later in the syncing logic.
          id = MediaProgress.UNKNOWN_ID,
          userId = userSession.requiredUserId,
          libraryItemId = libraryItemId,
          episodeId = null,
          mediaItemId = libraryItem.mediaId,
          mediaItemType = libraryItem.mediaType,
          duration = libraryItem.durationInMillis.milliseconds.toDouble(DurationUnit.SECONDS),
          progress = 1.0,
          currentTime = 0.0,
          isFinished = true,
          hideFromContinueListening = true,
          ebookLocation = null,
          ebookProgress = null,
          finishedAt = fatherTime.nowInEpochMillis(),
          lastUpdate = fatherTime.nowInEpochMillis(),
          startedAt = fatherTime.nowInEpochMillis(),
        )

        withContext(dispatcherProvider.databaseWrite) {
          db.mediaProgressQueries.insert(
            newMediaProgress,
          )
        }

        // This is heavy handed and duplicative as it will force another update request
        // followed by a fetch to hydrate its ACTUAL id.
        mediaProgressSynchronizer.sync(newMediaProgress.asDomainModel(), force = true)

        MediaProgressStore.ibark { "Created new finished progress for $libraryItemId" }
      } else {
        MediaProgressStore.ebark { "Error marking local finished for libraryItemId $libraryItemId" }
      }
    }.onFailure {
      MediaProgressStore.ebark { "Error marking finished for libraryItemId $libraryItemId" }
    }
  }

  override suspend fun markNotFinished(libraryItemId: LibraryItemId) {
    // First we just fetch the existing media progressId for the given library item id
    val mediaProgressId = db.mediaProgressQueries
      .getMediaProgressId(userSession.requiredUserId, libraryItemId)
      .awaitAsOneOrNull()

    // If it exists and is not an un-synced Id
    if (mediaProgressId != null && mediaProgressId != MediaProgress.UNKNOWN_ID) {
      api.deleteMediaProgress(mediaProgressId)
        .onSuccess {
          withContext(dispatcherProvider.databaseWrite) {
            db.mediaProgressQueries
              .delete(userSession.requiredUserId, libraryItemId)
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
        update = MediaProgressUpdatePayload(
          isFinished = false,
          progress = 0f,
          currentTime = 0f,
          hideFromContinueListening = true,
        ),
      ).onSuccess {
        // Marking as "Not Finished" is effectively deleting it, so let's just remove
        // and let a future sync handle any updated sync.
        withContext(dispatcherProvider.databaseWrite) {
          db.mediaProgressQueries
            .delete(userSession.requiredUserId, libraryItemId)
        }
      }.onFailure {
        MediaProgressStore.ebark { "Error marking not finished for libraryItemId $libraryItemId" }
      }
    }
  }
}

package app.campfire.user

import app.campfire.CampfireDatabase
import app.campfire.account.api.UserSessionManager
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.session.UserSession
import app.campfire.core.session.userId
import app.campfire.core.time.FatherTime
import app.campfire.data.mapping.asDbModel
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.envelopes.MediaProgressUpdatePayload
import app.campfire.user.api.MediaProgressRepository
import app.campfire.user.mediaprogress.MediaProgressSynchronizer
import app.campfire.user.mediaprogress.store.MediaProgressStore
import app.campfire.user.mediaprogress.store.MediaProgressStore.Operation
import app.campfire.user.mediaprogress.store.MediaProgressStore.Output
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.impl.extensions.get

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStoreApi::class)
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
class StoreMediaProgressRepository(
  private val userSessionManager: UserSessionManager,
  private val storeFactory: MediaProgressStore.Factory,
  private val db: CampfireDatabase,
  private val api: AudioBookShelfApi,
  private val mediaProgressSynchronizer: MediaProgressSynchronizer,
  private val fatherTime: FatherTime,
  private val dispatcherProvider: DispatcherProvider,
) : MediaProgressRepository {

  private val store: Store<Operation, Output> by lazy { storeFactory.create() }

  override fun observeProgress(libraryItemId: LibraryItemId): Flow<MediaProgress?> {
    return userSessionManager.observe()
      .filterIsInstance<UserSession.LoggedIn>()
      .flatMapLatest { session ->
        val request = StoreReadRequest.cached(Operation.Query.One(session.user.id, libraryItemId), false)
        store.stream(request)
          .onEach { response ->
            MediaProgressStore.ibark { "observeProgress --> $response" }
          }
          .map { it.dataOrNull() }
          .filterNotNull()
          .map { it.requireSingle() }
      }
  }

  override fun observeAllProgress(): Flow<List<MediaProgress>> {
    return userSessionManager.observe()
      .filterIsInstance<UserSession.LoggedIn>()
      .flatMapLatest { session ->
        val request = StoreReadRequest.cached(Operation.Query.All(session.user.id), false)
        store.stream(request)
          .onEach { response ->
            MediaProgressStore.ibark { "observeAllProgress --> $response" }
          }
          .map { it.dataOrNull() }
          .filterNotNull()
          .map { it.requireCollection() }
      }
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
    val currentUserId = userSessionManager.current.userId!!
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
      val currentUserId = userSessionManager.current.userId!!
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
      } else {
        MediaProgressStore.ebark { "Error marking local finished for libraryItemId $libraryItemId" }
      }
    }.onFailure {
      MediaProgressStore.ebark { "Error marking finished for libraryItemId $libraryItemId" }
    }
  }

  override suspend fun markNotFinished(libraryItemId: LibraryItemId) {
    api.updateMediaProgress(
      libraryItemId = libraryItemId,
      update = MediaProgressUpdatePayload(
        isFinished = false,
      ),
    ).onSuccess {
      val currentUserId = userSessionManager.current.userId!!
      val existing = store.get(Operation.Query.One(currentUserId, libraryItemId))
        .requireSingle()
      if (existing != null && existing.id != MediaProgress.UNKNOWN_ID) {
        withContext(dispatcherProvider.databaseWrite) {
          db.mediaProgressQueries.markNotFinished(
            timestamp = fatherTime.nowInEpochMillis(),
            userId = currentUserId,
            libraryItemId = libraryItemId,
          )
        }
        MediaProgressStore.ibark { "MediaProgress[$libraryItemId] marked NOT finished!" }
      } else {
        MediaProgressStore.ebark { "Error marking local not finished for libraryItemId $libraryItemId" }
      }
    }.onFailure {
      MediaProgressStore.ebark { "Error marking not finished for libraryItemId $libraryItemId" }
    }
  }
}

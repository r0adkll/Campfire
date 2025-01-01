package app.campfire.user

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.user.api.MediaProgressRepository
import app.campfire.user.api.UserRepository
import app.campfire.user.progress.MediaProgressStore
import app.campfire.user.progress.MediaProgressStore.Operation
import app.campfire.user.progress.MediaProgressStore.Output
import app.campfire.user.progress.MediaProgressWriteResponse
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.MutableStore
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreWriteRequest
import org.mobilenativefoundation.store.store5.StoreWriteResponse

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStoreApi::class)
@ContributesBinding(UserScope::class)
@SingleIn(UserScope::class)
@Inject
class StoreMediaProgressRepository(
  private val storeFactory: MediaProgressStore.Factory,
  private val userRepository: UserRepository,
) : MediaProgressRepository {

  private val store: MutableStore<Operation, Output> by lazy { storeFactory.create() }

  override fun observeProgress(libraryItemId: LibraryItemId): Flow<MediaProgress> {
    val request = StoreReadRequest.cached(Operation.Query.One(libraryItemId), false)
    return store.stream<Output>(request)
      .onEach { response ->
        response.throwIfError()
        MediaProgressStore.ibark { "observeProgress --> $response" }
      }
      .map { it.dataOrNull() }
      .filterNotNull()
      .map { it.requireSingle() }
  }

  override fun observeAllProgress(): Flow<List<MediaProgress>> {
    return userRepository.observeCurrentUser()
      .flatMapLatest { user ->
        val request = StoreReadRequest.cached(Operation.Query.All(user.id), false)
        store.stream<Output>(request)
          .onEach { response ->
            MediaProgressStore.ibark { "observeAllProgress --> $response" }
          }
          .map { it.dataOrNull() }
          .filterNotNull()
          .map { it.requireCollection() }
      }
  }

  override suspend fun updateProgress(newProgress: MediaProgress) {
    MediaProgressStore.ibark { "updateProgress <-- $newProgress" }
    val request: StoreWriteRequest<Operation, Output, MediaProgressWriteResponse> = StoreWriteRequest.of(
      key = Operation.Mutation.Update.UpsertOne(newProgress),
      value = Output.Single(newProgress),
    )

    writeRequest(request)
  }

  override suspend fun deleteProgress(libraryItemId: LibraryItemId) {
    MediaProgressStore.ibark { "deleteProgress <-- $libraryItemId" }
    val currentUser = userRepository.getCurrentUser()
    val request: StoreWriteRequest<Operation, Output, MediaProgressWriteResponse> = StoreWriteRequest.of(
      key = Operation.Mutation.Delete.One(currentUser.id, libraryItemId),
      value = Output.Collection(emptyList()),
    )

    writeRequest(request)
  }

  private suspend fun writeRequest(request: StoreWriteRequest<Operation, Output, MediaProgressWriteResponse>) {
    when (val response = store.write(request)) {
      is StoreWriteResponse.Error.Exception -> MediaProgressStore.ebark(response.error) {
        "Error writing to store: $request"
      }
      is StoreWriteResponse.Error.Message -> MediaProgressStore.ebark {
        "Error writing to store: ${response.message}\nRequest = $request"
      }
      is StoreWriteResponse.Success.Typed<*> -> MediaProgressStore.ibark {
        "Store write success: ${response.value}\nRequest = $request"
      }
      is StoreWriteResponse.Success.Untyped -> MediaProgressStore.ibark {
        "Store write success: ${response.value}\nRequest = $request"
      }
    }
  }
}

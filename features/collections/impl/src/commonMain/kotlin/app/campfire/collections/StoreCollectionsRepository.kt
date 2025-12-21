package app.campfire.collections

import app.campfire.CampfireDatabase
import app.campfire.account.api.UrlHydrator
import app.campfire.collections.api.CollectionsRepository
import app.campfire.collections.store.CollectionsStore
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.Collection
import app.campfire.core.model.CollectionId
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.store.debugLogging
import app.campfire.user.api.UserRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store.store5.StoreReadResponseOrigin
import org.mobilenativefoundation.store.store5.StoreWriteRequest
import org.mobilenativefoundation.store.store5.StoreWriteResponse

@OptIn(ExperimentalStoreApi::class, ExperimentalCoroutinesApi::class)
@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class StoreCollectionsRepository(
  private val userRepository: UserRepository,
  private val db: CampfireDatabase,
  private val urlHydrator: UrlHydrator,
  private val storeFactory: CollectionsStore.Factory,
  private val dispatcherProvider: DispatcherProvider,
) : CollectionsRepository {

  private val collectionsStore by lazy { storeFactory.create() }

  override fun observeAllCollections(): Flow<List<Collection>> {
    return userRepository.observeCurrentUser()
      .flatMapLatest { user ->

        val operation = CollectionsStore.Operation.All(user.id, user.selectedLibraryId)
        val request = StoreReadRequest.cached(operation, refresh = true)

        collectionsStore.stream<StoreReadResponse<CollectionsStore.Output>>(request)
          .debugLogging("CollectionsStore::observeAllCollections")
          .filterNot { it is StoreReadResponse.Loading || it is StoreReadResponse.NoNewData }
          .mapNotNull { response ->
            response.dataOrNull()?.let { output ->
              // If the response is empty, and from the SoT then lets just return null and wait
              // for the network request to return.
              if (output.isEmpty() && response.origin == StoreReadResponseOrigin.SourceOfTruth) {
                CollectionsStore.dbark { "Output is empty and response is from SoT, force network fetch." }
                return@mapNotNull null
              }

              (output as CollectionsStore.Output.Collection).collections
            }
          }
      }
  }

  override fun observeCollection(collectionId: CollectionId): Flow<Collection> {
    return userRepository.observeCurrentUser()
      .flatMapLatest { user ->
        val operation = CollectionsStore.Operation.Single(user.id, user.selectedLibraryId, collectionId)
        val request = StoreReadRequest.cached(operation, refresh = false)

        collectionsStore.stream<StoreReadResponse<CollectionsStore.Output>>(request)
          .debugLogging("CollectionsStore::observeCollection")
          .filterNot { it is StoreReadResponse.Loading || it is StoreReadResponse.NoNewData }
          .mapNotNull { response ->
            val output = response.dataOrNull() as? CollectionsStore.Output.Single
            output?.collection
          }
      }
  }

  override fun observeCollectionItems(collectionId: CollectionId): Flow<List<LibraryItem>> {
    return db.libraryItemsQueries
      .selectForCollection(collectionId)
      .asFlow()
      .mapToList(dispatcherProvider.databaseRead)
      .mapLatest { selectForCollection ->
        selectForCollection
          .map { it.asDomainModel(urlHydrator) }
      }
  }

  override suspend fun createCollection(
    name: String,
    bookIds: List<String>,
    description: String?,
  ): Result<CollectionId> {
    val currentUser = userRepository.getCurrentUser()

    val operation = CollectionsStore.Operation.Mutation.Create(
      userId = currentUser.id,
      name = name,
      description = description,
      libraryId = currentUser.selectedLibraryId,
      bookIds = bookIds,
    )

    val response = collectionsStore.write(
      request = StoreWriteRequest.of(operation, CollectionsStore.Output.Collection(emptyList())),
    )

    return when (response) {
      is StoreWriteResponse.Success -> {
        CollectionsStore.ibark { "Collection Create -> $response" }
        Result.success("TBD")
      }

      is StoreWriteResponse.Error.Message -> Result.failure(Exception(response.message))
      is StoreWriteResponse.Error.Exception -> Result.failure(response.error)
    }
  }

  override suspend fun updateCollection(
    collectionId: CollectionId,
    name: String?,
    description: String?,
  ): Result<Unit> {
    val currentUser = userRepository.getCurrentUser()

    val operation = CollectionsStore.Operation.Mutation.Update(
      userId = currentUser.id,
      id = collectionId,
      name = name,
      description = description,
    )

    val response = collectionsStore.write(
      request = StoreWriteRequest.of(operation, CollectionsStore.Output.Collection(emptyList())),
    )

    return when (response) {
      is StoreWriteResponse.Success -> {
        CollectionsStore.ibark { "Collection Updated -> $response" }
        Result.success(Unit)
      }

      is StoreWriteResponse.Error.Message -> Result.failure(Exception(response.message))
      is StoreWriteResponse.Error.Exception -> Result.failure(response.error)
    }
  }

  override suspend fun deleteCollection(collectionId: CollectionId): Result<Unit> {
    val currentUser = userRepository.getCurrentUser()

    val operation = CollectionsStore.Operation.Mutation.Delete(
      userId = currentUser.id,
      id = collectionId,
    )

    val response = collectionsStore.write(
      request = StoreWriteRequest.of(operation, CollectionsStore.Output.Collection(emptyList())),
    )

    return when (response) {
      is StoreWriteResponse.Success -> {
        CollectionsStore.ibark { "Collection Deleted -> $response" }
        Result.success(Unit)
      }

      is StoreWriteResponse.Error.Message -> Result.failure(Exception(response.message))
      is StoreWriteResponse.Error.Exception -> Result.failure(response.error)
    }
  }

  override suspend fun addToCollection(bookId: LibraryItemId, collectionId: CollectionId): Result<Unit> {
    val currentUser = userRepository.getCurrentUser()

    val operation = CollectionsStore.Operation.Mutation.Add(
      userId = currentUser.id,
      collectionId = collectionId,
      bookId = bookId,
    )

    val response = collectionsStore.write(
      request = StoreWriteRequest.of(operation, CollectionsStore.Output.Collection(emptyList())),
    )

    return when (response) {
      is StoreWriteResponse.Success -> {
        CollectionsStore.ibark { "Book Added -> $response" }
        Result.success(Unit)
      }

      is StoreWriteResponse.Error.Message -> Result.failure(Exception(response.message))
      is StoreWriteResponse.Error.Exception -> Result.failure(response.error)
    }
  }

  override suspend fun removeFromCollection(bookIds: List<LibraryItemId>, collectionId: CollectionId): Result<Unit> {
    val currentUser = userRepository.getCurrentUser()

    val operation = CollectionsStore.Operation.Mutation.Remove(
      userId = currentUser.id,
      collectionId = collectionId,
      bookIds = bookIds,
    )

    val response = collectionsStore.write(
      request = StoreWriteRequest.of(operation, CollectionsStore.Output.Collection(emptyList())),
    )

    return when (response) {
      is StoreWriteResponse.Success -> {
        CollectionsStore.ibark { "Book Removed -> $response" }
        Result.success(Unit)
      }

      is StoreWriteResponse.Error.Message -> Result.failure(Exception(response.message))
      is StoreWriteResponse.Error.Exception -> Result.failure(response.error)
    }
  }
}

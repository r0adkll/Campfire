package app.campfire.collections

import app.campfire.CampfireDatabase
import app.campfire.account.api.TokenHydrator
import app.campfire.collections.api.CollectionsRepository
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.Collection
import app.campfire.core.model.CollectionId
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItem
import app.campfire.core.session.UserSession
import app.campfire.data.CollectionsBookJoin
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.asFetcherResult
import app.campfire.data.mapping.dao.LibraryItemDao
import app.campfire.network.AudioBookShelfApi
import app.campfire.user.api.UserRepository
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.StoreBuilder
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class StoreCollectionsRepository(
  private val userSession: UserSession,
  private val userRepository: UserRepository,
  private val api: AudioBookShelfApi,
  private val db: CampfireDatabase,
  private val libraryItemDao: LibraryItemDao,
  private val tokenHydrator: TokenHydrator,
  private val dispatcherProvider: DispatcherProvider,
) : CollectionsRepository {

  @OptIn(ExperimentalCoroutinesApi::class)
  private val collectionsStore = StoreBuilder.from(
    fetcher = Fetcher.ofResult { libraryId: LibraryId -> api.getCollections(libraryId).asFetcherResult() },
    sourceOfTruth = SourceOfTruth.of(
      reader = { libraryId: LibraryId ->
        db.collectionsQueries.selectByLibraryId(libraryId)
          .asFlow()
          .mapToList(dispatcherProvider.databaseRead)
          .mapLatest { collections ->
            collections.associateWith { c ->
              db.libraryItemsQueries
                .selectForCollection(c.id)
                .awaitAsList()
            }
          }
      },
      writer = { _, collections ->
        withContext(dispatcherProvider.databaseWrite) {
          db.transaction {
            collections.forEach { collection ->
              // Insert collection
              db.collectionsQueries.insert(collection.asDbModel())

              // Insert the collection books
              collection.books.forEach { book ->

                // These books are expanded objects, so they should be safe to insert/replace
                libraryItemDao.insert(
                  item = book,
                  asTransaction = false,
                )

                // Insert junction entry
                db.collectionsBookJoinQueries.insert(
                  CollectionsBookJoin(
                    collectionsId = collection.id,
                    libraryItemId = book.id,
                  ),
                )
              }
            }
          }
        }
      },
      delete = { libraryId: LibraryId ->
        withContext(dispatcherProvider.databaseWrite) {
          db.collectionsQueries.deleteForLibraryId(libraryId)
        }
      },
    ),
  ).build()

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeAllCollections(): Flow<List<Collection>> {
    return userRepository.observeCurrentUser()
      .flatMapLatest { user ->
        collectionsStore.stream(StoreReadRequest.cached(user.selectedLibraryId, refresh = true))
          .filterNot { it is StoreReadResponse.Loading || it is StoreReadResponse.NoNewData }
          .mapNotNull { response ->
            response.dataOrNull()?.let { collections ->
              collections.entries.map { (c, books) ->
                c.asDomainModel(books.map { it.asDomainModel(tokenHydrator) })
              }
            }
          }
      }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeCollectionItems(collectionId: CollectionId): Flow<List<LibraryItem>> {
    return db.libraryItemsQueries
      .selectForCollection(collectionId)
      .asFlow()
      .mapToList(dispatcherProvider.databaseRead)
      .mapLatest { selectForCollection ->
        selectForCollection
          .map { it.asDomainModel(tokenHydrator) }
      }
  }
}

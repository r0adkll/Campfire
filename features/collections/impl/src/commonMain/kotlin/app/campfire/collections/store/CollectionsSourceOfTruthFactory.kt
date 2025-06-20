package app.campfire.collections.store

import app.campfire.CampfireDatabase
import app.campfire.account.api.TokenHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.Collection
import app.campfire.core.model.CollectionId
import app.campfire.core.model.LibraryId
import app.campfire.core.model.UserId
import app.campfire.core.time.FatherTime
import app.campfire.data.Collections
import app.campfire.data.CollectionsBookJoin
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.dao.LibraryItemDao
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import org.mobilenativefoundation.store.store5.SourceOfTruth

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionsSourceOfTruthFactory(
  private val db: CampfireDatabase,
  private val libraryItemDao: LibraryItemDao,
  private val dispatcherProvider: DispatcherProvider,
  private val tokenHydrator: TokenHydrator,
  private val fatherTime: FatherTime,
) {

  fun create(): SourceOfTruth<CollectionsStore.Operation, List<Collection>, CollectionsStore.Output> {
    return SourceOfTruth.of(
      reader = { operation -> handleRead(operation) },
      writer = { operation, collections -> handleWrite(operation, collections) },
      delete = { operation ->
        require(operation is CollectionsStore.Operation.Mutation.Delete)
        handleDelete(operation)
      },
    )
  }

  private fun handleRead(operation: CollectionsStore.Operation): Flow<CollectionsStore.Output> {
    require(operation is CollectionsStore.Operation.All || operation is CollectionsStore.Operation.Single)
    return when (operation) {
      is CollectionsStore.Operation.All -> readAll(operation.userId, operation.libraryId)
      is CollectionsStore.Operation.Single -> readSingle(operation.collectionId)
      else -> throw IllegalArgumentException("Unknown operation: $operation")
    }
  }

  private fun readAll(userId: UserId, libraryId: LibraryId): Flow<CollectionsStore.Output.Collection> {
    return db.collectionsQueries.selectByLibraryId(libraryId, userId)
      .asFlow()
      .mapToList(dispatcherProvider.databaseRead)
      .mapLatest { collections ->
        collections.associateWith { c ->
          db.libraryItemsQueries
            .selectForCollection(c.id)
            .awaitAsList()
        }
      }
      .mapLatest { collectionMap ->
        collectionMap.entries.map { (c, books) ->
          c.asDomainModel(books.map { it.asDomainModel(tokenHydrator) })
        }
      }
      .map { CollectionsStore.Output.Collection(it) }
  }

  private fun readSingle(collectionId: CollectionId): Flow<CollectionsStore.Output.Single> {
    return db.collectionsQueries.selectById(collectionId)
      .asFlow()
      .mapToOneOrNull(dispatcherProvider.databaseRead)
      .mapNotNull { collection ->
        collection?.let { c ->
          val libraryItems = db.libraryItemsQueries
            .selectForCollection(c.id)
            .awaitAsList()
            .map { it.asDomainModel(tokenHydrator) }

          c.asDomainModel(libraryItems)
        }
      }
      .map { CollectionsStore.Output.Single(it) }
  }

  private suspend fun handleWrite(
    operation: CollectionsStore.Operation,
    collections: List<Collection> = emptyList(),
  ) {
    when (operation) {
      is CollectionsStore.Operation.All -> writeAll(operation.userId, operation.libraryId, collections)
      is CollectionsStore.Operation.Single -> writeSingle(operation.userId, operation.libraryId, collections.first())
      is CollectionsStore.Operation.Mutation.Create -> writeCreate(operation)
      is CollectionsStore.Operation.Mutation.Update -> writeUpdate(operation)
      is CollectionsStore.Operation.Mutation.Add -> handleAdd(operation)
      is CollectionsStore.Operation.Mutation.Remove -> handleRemove(operation)
      is CollectionsStore.Operation.Mutation.Delete -> handleDelete(operation)
    }
  }

  private suspend fun writeAll(
    userId: UserId,
    libraryId: LibraryId,
    collections: List<Collection>,
  ) = withContext(dispatcherProvider.databaseWrite) {
    db.transaction {
      // Delete any existing entries that no longer exist
      db.collectionsQueries.deleteOld(collections.map { it.id })

      collections.forEach { collection ->
        // Insert collection
        db.collectionsQueries.insert(collection.asDbModel(userId, libraryId))

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

  private suspend fun writeSingle(
    userId: UserId,
    libraryId: LibraryId,
    collection: Collection,
  ) = withContext(dispatcherProvider.databaseWrite) {
    db.transaction {
      // Insert collection
      db.collectionsQueries.insert(collection.asDbModel(userId, libraryId))

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

  private suspend fun writeCreate(
    mutation: CollectionsStore.Operation.Mutation.Create,
  ) = withContext(dispatcherProvider.databaseWrite) {
    db.transaction {
      db.collectionsQueries
        .insert(
          Collections(
            id = mutation.creationId.toHexDashString(),
            userId = mutation.userId,
            name = mutation.name,
            description = mutation.description,
            cover = null,
            coverFullPath = null,
            libraryId = mutation.libraryId,
            updatedAt = fatherTime.nowInEpochMillis(),
            createdAt = fatherTime.nowInEpochMillis(),
          ),
        )

      mutation.bookIds.forEach { bookId ->
        db.collectionsBookJoinQueries.insert(
          CollectionsBookJoin(
            collectionsId = mutation.creationId.toHexDashString(),
            libraryItemId = bookId,
          ),
        )
      }
    }
  }

  private suspend fun writeUpdate(
    mutation: CollectionsStore.Operation.Mutation.Update,
  ) = withContext(dispatcherProvider.databaseWrite) {
    db.transaction {
      if (mutation.name != null) {
        db.collectionsQueries.updateName(
          name = mutation.name,
          updatedAt = fatherTime.nowInEpochMillis(),
          id = mutation.id,
        )
      }
      if (mutation.description != null) {
        db.collectionsQueries.updateDescription(
          description = mutation.description,
          updatedAt = fatherTime.nowInEpochMillis(),
          id = mutation.id,
        )
      }
    }
  }

  private suspend fun handleAdd(
    mutation: CollectionsStore.Operation.Mutation.Add,
  ) = withContext(dispatcherProvider.databaseWrite) {
    db.collectionsBookJoinQueries.insert(
      CollectionsBookJoin(
        collectionsId = mutation.collectionId,
        libraryItemId = mutation.bookId,
      ),
    )
  }

  private suspend fun handleRemove(
    mutation: CollectionsStore.Operation.Mutation.Remove,
  ) = withContext(dispatcherProvider.databaseWrite) {
    db.collectionsBookJoinQueries.deleteForItem(
      collectionsId = mutation.collectionId,
      libraryItemId = mutation.bookId,
    )
  }

  private suspend fun handleDelete(
    mutation: CollectionsStore.Operation.Mutation.Delete,
  ) = withContext(dispatcherProvider.databaseWrite) {
    db.transaction {
      db.collectionsBookJoinQueries.delete(mutation.id)
      db.collectionsQueries.delete(mutation.id)
    }
  }
}

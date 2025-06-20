package app.campfire.collections.store

import app.campfire.CampfireDatabase
import app.campfire.collections.store.CollectionsStore.Operation
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.UserId
import app.campfire.data.CollectionsBookJoin
import app.campfire.data.mapping.asDbModel
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.models.Collection
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import kotlin.uuid.Uuid
import org.mobilenativefoundation.store.store5.OnUpdaterCompletion
import org.mobilenativefoundation.store.store5.Updater
import org.mobilenativefoundation.store.store5.UpdaterResult

class CollectionsUpdaterFactory(
  private val api: AudioBookShelfApi,
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
) {

  fun create(): Updater<Operation, CollectionsStore.Output, CollectionsStore.Update> {
    return Updater.by(
      post = { operation, output ->
        require(operation is Operation.Mutation)
        when (operation) {
          is Operation.Mutation.Create -> handleCreate(operation)
          is Operation.Mutation.Update -> handleUpdate(operation)
          is Operation.Mutation.Add -> handleAdd(operation)
          is Operation.Mutation.Remove -> handleRemove(operation)
          is Operation.Mutation.Delete -> handleDelete(operation)
        }
      },
      onCompletion = OnUpdaterCompletion(
        onSuccess = { result ->
        },
        onFailure = {
        },
      ),
    )
  }

  private suspend fun handleCreate(mutation: Operation.Mutation.Create): UpdaterResult {
    val result = api.createCollection(
      libraryId = mutation.libraryId,
      name = mutation.name,
      description = mutation.description,
      bookIds = mutation.bookIds,
    )

    return if (result.isSuccess) {
      updateLocalCreate(mutation.userId, mutation.creationId, result.getOrThrow())
      UpdaterResult.Success.Typed(result.getOrThrow())
    } else {
      result.exceptionOrNull()?.let { UpdaterResult.Error.Exception(it) }
        ?: UpdaterResult.Error.Message("Unable to create the collection")
    }
  }

  private suspend fun updateLocalCreate(
    userId: UserId,
    creationId: Uuid,
    collection: Collection,
  ) {
    val existing = db.collectionsQueries
      .selectById(creationId.toHexDashString())
      .awaitAsOneOrNull()

    if (existing != null) {
      db.transaction {
        // Insert a copy, with the real id
        db.collectionsQueries.insert(collection.asDbModel(userId))

        // Copy over the junction entries
        collection.books.forEach { book ->
          db.collectionsBookJoinQueries.insert(
            CollectionsBookJoin(collection.id, book.id),
          )
        }

        // Delete the old stuff
        db.collectionsQueries.delete(creationId.toHexDashString())
        db.collectionsBookJoinQueries.delete(creationId.toHexDashString())
      }
    }
  }

  private suspend fun handleUpdate(mutation: Operation.Mutation.Update): UpdaterResult {
    val result = api.updateCollection(
      collectionId = mutation.id,
      name = mutation.name,
      description = mutation.description,
    )

    return if (result.isSuccess) {
      UpdaterResult.Success.Typed(result.getOrThrow())
    } else {
      result.exceptionOrNull()?.let { UpdaterResult.Error.Exception(it) }
        ?: UpdaterResult.Error.Message("Unable to update the collection")
    }
  }

  private suspend fun handleAdd(mutation: Operation.Mutation.Add): UpdaterResult {
    val result = api.addBookToCollection(mutation.collectionId, mutation.bookId)

    return if (result.isSuccess) {
      UpdaterResult.Success.Typed(result.getOrThrow())
    } else {
      result.exceptionOrNull()?.let { UpdaterResult.Error.Exception(it) }
        ?: UpdaterResult.Error.Message("Unable to add the book to the collection")
    }
  }

  private suspend fun handleRemove(mutation: Operation.Mutation.Remove): UpdaterResult {
    val result = api.removeBookFromCollection(mutation.collectionId, mutation.bookId)

    return if (result.isSuccess) {
      UpdaterResult.Success.Typed(result.getOrThrow())
    } else {
      result.exceptionOrNull()?.let { UpdaterResult.Error.Exception(it) }
        ?: UpdaterResult.Error.Message("Unable to add the book to the collection")
    }
  }

  private suspend fun handleDelete(mutation: Operation.Mutation.Delete): UpdaterResult {
    val result = api.deleteCollection(mutation.id)
    return if (result.isSuccess) {
      UpdaterResult.Success.Untyped(Unit)
    } else {
      result.exceptionOrNull()?.let { UpdaterResult.Error.Exception(it) }
        ?: UpdaterResult.Error.Message("Unable to delete the collection")
    }
  }
}

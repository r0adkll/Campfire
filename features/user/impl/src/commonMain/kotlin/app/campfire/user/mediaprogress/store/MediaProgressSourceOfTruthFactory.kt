package app.campfire.user.mediaprogress.store

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.UserId
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDomainModel
import app.campfire.user.mediaprogress.store.MediaProgressStore.Operation
import app.campfire.user.mediaprogress.store.MediaProgressStore.Output
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
      MediaProgressStore.ibark { "SourceOfTruth[reader]: $operation" }
      when (operation) {
        is Operation.Query.All -> observeAll(operation.userId)
        is Operation.Query.One -> observeByLibraryItemId(operation.userId, operation.libraryItemId)
      }
    },
    writer = { operation, output ->
      MediaProgressStore.ibark { "SourceOfTruth[writer]: $operation, output: $output" }
      handleWrite(operation, output)
    },
    delete = { operation ->
      require(operation is Operation.Query.One)
      MediaProgressStore.ibark { "SourceOfTruth[delete]: $operation" }
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

  private fun observeByLibraryItemId(userId: UserId, libraryItemId: LibraryItemId): Flow<Output.Single> {
    return db.mediaProgressQueries
      .selectForLibraryItem(userId, libraryItemId)
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
      is Operation.Query.One -> deleteSingle(operation.userId, operation.libraryItemId)
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
        db.mediaProgressQueries.insert(item.asDbModel())
      }
    }
  }

  private suspend fun writeSingle(output: Output.Single) = withContext(dispatcherProvider.databaseWrite) {
    output.item?.let { item ->
      db.mediaProgressQueries.insert(item.asDbModel())
    }
  }

  private suspend fun deleteSingle(userId: UserId, libraryItemId: LibraryItemId) {
    withContext(dispatcherProvider.databaseWrite) {
      db.mediaProgressQueries.delete(userId, libraryItemId)
    }
  }

  private suspend fun deleteAll(userId: UserId) {
    withContext(dispatcherProvider.databaseWrite) {
      db.mediaProgressQueries.deleteForUser(userId)
    }
  }
}

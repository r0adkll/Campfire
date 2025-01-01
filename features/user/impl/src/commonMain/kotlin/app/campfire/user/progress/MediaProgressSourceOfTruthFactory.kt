package app.campfire.user.progress

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.UserId
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDomainModel
import app.campfire.user.progress.MediaProgressStore.Operation
import app.campfire.user.progress.MediaProgressStore.Output
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
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
        is Operation.Query.One -> observeByLibraryItemId(operation.libraryItemId)
        else -> flowOf(null)
      }
    },
    writer = { operation, output ->
      MediaProgressStore.ibark { "SourceOfTruth[writer]: $operation, output: $output" }
      handleWrite(operation, output)
    },
    delete = { operation ->
      MediaProgressStore.ibark { "SourceOfTruth[delete]: $operation" }
      handleWrite(operation)
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

  private fun observeByLibraryItemId(libraryItemId: LibraryItemId): Flow<Output.Single> {
    return db.mediaProgressQueries
      .selectForLibraryItem(libraryItemId)
      .asFlow()
      .mapToOneOrNull(dispatcherProvider.databaseRead)
      .filterNotNull()
      .map { Output.Single(it.asDomainModel()) }
  }

  private suspend fun handleWrite(operation: Operation, output: Output = Output.Collection(emptyList())) {
    when (operation) {
      is Operation.Mutation.Update.UpsertMany -> writeAll(Output.Collection(operation.items))
      is Operation.Mutation.Update.UpsertOne -> writeSingle(Output.Single(operation.item))
      is Operation.Query.All -> writeOutput(output)
      is Operation.Query.One -> writeOutput(output)
      is Operation.Mutation.Delete.One -> deleteByLibraryItemId(operation.libraryItemId)
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
    db.mediaProgressQueries.insert(output.item.asDbModel())
  }

  private suspend fun deleteByLibraryItemId(libraryItemId: LibraryItemId) {
    withContext(dispatcherProvider.databaseWrite) {
      db.mediaProgressQueries.delete(libraryItemId)
    }
  }
}

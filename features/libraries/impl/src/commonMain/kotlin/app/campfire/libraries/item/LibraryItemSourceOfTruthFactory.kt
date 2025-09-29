package app.campfire.libraries.item

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.data.mapping.dao.LibraryItemDao
import app.campfire.data.mapping.model.mapToLibraryItem
import app.campfire.network.models.LibraryItemExpanded as NetworkLibraryItem
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import org.mobilenativefoundation.store.store5.SourceOfTruth

class LibraryItemSourceOfTruthFactory(
  private val db: CampfireDatabase,
  private val libraryItemDao: LibraryItemDao,
  private val dispatcherProvider: DispatcherProvider,
) {

  fun create(): SourceOfTruth<LibraryItemId, NetworkLibraryItem, LibraryItem> {
    return SourceOfTruth.of(
      reader = { libraryItemId -> readLibraryItem(libraryItemId) },
      writer = { libraryItemId, item -> writeItem(libraryItemId, item) },
      delete = { libraryItemId -> deleteItem(libraryItemId) },
    )
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private fun readLibraryItem(libraryItemId: LibraryItemId): Flow<LibraryItem?> {
    return db.libraryItemsQueries
      .selectForId(libraryItemId, ::mapToLibraryItem)
      .asFlow()
      .mapToOneOrNull(dispatcherProvider.databaseRead)
      .mapLatest { item ->
        if (item == null) return@mapLatest null
        libraryItemDao.hydrateItem(item)
      }
  }

  private suspend fun writeItem(libraryItemId: LibraryItemId, item: NetworkLibraryItem) {
    libraryItemDao.insert(item)
  }

  private suspend fun deleteItem(libraryItemId: LibraryItemId) {
    withContext(dispatcherProvider.databaseWrite) {
      db.libraryItemsQueries.deleteForId(libraryItemId)
    }
  }
}

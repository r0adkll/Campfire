package app.campfire.libraries.item

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaType
import app.campfire.data.mapping.dao.LibraryItemDao
import app.campfire.data.mapping.model.mapToLibraryItem
import app.campfire.data.mapping.model.mapToPodcastLibraryItem
import app.campfire.network.models.LibraryItemExpanded as NetworkLibraryItem
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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

  /**
   * Read flow dispatches on the libraryItem's mediaType — book items are joined with the
   * `media` table, podcast items with `podcastMedia`. Each path runs the dao's matching
   * hydrate function so the resulting domain item carries the correct [Media] subtype.
   */
  @OptIn(ExperimentalCoroutinesApi::class)
  private fun readLibraryItem(libraryItemId: LibraryItemId): Flow<LibraryItem?> {
    return db.libraryItemsQueries
      .selectMediaTypeForId(libraryItemId)
      .asFlow()
      .mapToOneOrNull(dispatcherProvider.databaseRead)
      .flatMapLatest { mediaType ->
        when (mediaType) {
          MediaType.Book ->
            db.libraryItemsQueries
              .selectForId(libraryItemId, ::mapToLibraryItem)
              .asFlow()
              .mapToOneOrNull(dispatcherProvider.databaseRead)
              .mapLatest { item -> item?.let { libraryItemDao.hydrateItem(it) } }
          MediaType.Podcast ->
            db.libraryItemsQueries
              .selectForPodcastId(libraryItemId, ::mapToPodcastLibraryItem)
              .asFlow()
              .mapToOneOrNull(dispatcherProvider.databaseRead)
              .mapLatest { item -> item?.let { libraryItemDao.hydratePodcastItem(it) } }
          null -> flowOf(null)
        }
      }
  }

  private suspend fun writeItem(libraryItemId: LibraryItemId, item: NetworkLibraryItem) {
    // The dao dispatcher routes Book vs Podcast variants into the right insert path.
    libraryItemDao.insert(item)
  }

  private suspend fun deleteItem(libraryItemId: LibraryItemId) {
    withContext(dispatcherProvider.databaseWrite) {
      db.libraryItemsQueries.deleteForId(libraryItemId)
    }
  }
}

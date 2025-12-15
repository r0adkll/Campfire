package app.campfire.home.store.shelf

import app.campfire.CampfireDatabase
import app.campfire.account.api.TokenHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.Author
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Series
import app.campfire.core.model.ShelfEntity
import app.campfire.core.model.ShelfType
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.dao.LibraryItemDao
import app.campfire.data.mapping.model.mapToLibraryItemWithProgress
import app.campfire.home.api.model.ShelfId
import app.campfire.home.store.shelf.ShelfStore.Key
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import org.mobilenativefoundation.store.store5.SourceOfTruth

@OptIn(ExperimentalCoroutinesApi::class)
class ShelfSourceOfTruthFactory(
  private val db: CampfireDatabase,
  private val libraryItemDao: LibraryItemDao,
  private val tokenHydrator: TokenHydrator,
  private val dispatcherProvider: DispatcherProvider,
) {

  fun create(): SourceOfTruth<Key, Unit, List<ShelfEntity>> {
    return SourceOfTruth.of(
      reader = { (shelfId, shelfType) ->
        when (shelfType) {
          ShelfType.BOOK,
          ShelfType.EPISODE,
          ShelfType.PODCAST,
          -> readLibraryItems(shelfId)

          ShelfType.SERIES -> readSeries(shelfId)
          ShelfType.AUTHOR -> readAuthors(shelfId)
        }
      },
      writer = { _, _ ->
        // Do nothing
      },
    )
  }

  private fun readLibraryItems(shelfId: ShelfId): Flow<List<LibraryItem>> {
    return db.libraryItemsQueries.selectForShelf(shelfId, ::mapToLibraryItemWithProgress)
      .asFlow()
      .mapToList(dispatcherProvider.databaseRead)
      .mapLatest { items -> items.map { libraryItemDao.hydrateItem(it) } }
  }

  private fun readSeries(shelfId: ShelfId): Flow<List<Series>> {
    return db.seriesQueries.selectByShelfId(shelfId)
      .asFlow()
      .mapToList(dispatcherProvider.databaseRead)
      .mapLatest { series ->
        val seriesWithBooks = series.associateWith { s ->
          db.libraryItemsQueries
            .selectForSeries(s.id)
            .awaitAsList()
        }

        seriesWithBooks.entries.map { (s, books) ->
          val sortedBooks = books
            .map { it.asDomainModel(tokenHydrator) }
            .sortedBy { it.media.metadata.seriesSequence?.sequence }

          s.asDomainModel(sortedBooks)
        }
      }
  }

  private fun readAuthors(shelfId: ShelfId): Flow<List<Author>> {
    return db.authorsQueries.selectForShelf(shelfId)
      .asFlow()
      .mapToList(dispatcherProvider.databaseRead)
      .mapLatest { authors ->
        authors.map { it.asDomainModel() }
      }
  }
}

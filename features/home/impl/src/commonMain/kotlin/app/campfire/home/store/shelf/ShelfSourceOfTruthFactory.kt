package app.campfire.home.store.shelf

import app.campfire.CampfireDatabase
import app.campfire.account.api.UrlHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.Author
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Series
import app.campfire.core.model.ShelfEntity
import app.campfire.core.model.ShelfType
import app.campfire.core.session.UserSession
import app.campfire.core.session.requiredUserId
import app.campfire.crashreporting.CrashReporter
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.dao.LibraryItemDao
import app.campfire.data.mapping.model.mapToEpisodeShelfRow
import app.campfire.data.mapping.model.mapToLibraryItemWithProgress
import app.campfire.data.mapping.model.mapToPodcastLibraryItem
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
  private val urlHydrator: UrlHydrator,
  private val dispatcherProvider: DispatcherProvider,
  private val userSession: UserSession,
) {

  fun create(): SourceOfTruth<Key, Unit, List<ShelfEntity>> {
    return SourceOfTruth.of(
      reader = { (shelfId, shelfType) ->
        when (shelfType) {
          ShelfType.BOOK -> readLibraryItems(shelfId)
          ShelfType.PODCAST -> readPodcastLibraryItems(shelfId)
          ShelfType.EPISODE -> readEpisodeShelfEntries(shelfId)
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
    return db.libraryItemsQueries
      .selectForShelf(
        userId = userSession.requiredUserId,
        shelfId = shelfId,
        mapper = ::mapToLibraryItemWithProgress,
      )
      .asFlow()
      .mapToList(dispatcherProvider.databaseRead)
      .mapLatest { items -> items.map { libraryItemDao.hydrateItem(it) } }
      .mapLatest { items -> items.dropDuplicates(shelfId) { it.id } }
  }

  /**
   * Hydrate podcast shelf entries (`PodcastShelf` — one entry per podcast). Each row is
   * the libraryItem joined with podcastMedia; episodes are loaded per-item by the dao.
   */
  private fun readPodcastLibraryItems(shelfId: ShelfId): Flow<List<LibraryItem>> {
    return db.libraryItemsQueries.selectForPodcastShelf(shelfId, ::mapToPodcastLibraryItem)
      .asFlow()
      .mapToList(dispatcherProvider.databaseRead)
      .mapLatest { items -> items.map { libraryItemDao.hydratePodcastItem(it) } }
      .mapLatest { items -> items.dropDuplicates(shelfId) { it.id } }
  }

  /**
   * Hydrate `episodes-recently-added`-style shelves where each entry pairs a podcast
   * libraryItem with the specific recent episode being highlighted.
   */
  private fun readEpisodeShelfEntries(shelfId: ShelfId): Flow<List<ShelfEntity.EpisodeShelfEntry>> {
    return db.libraryItemsQueries.selectForEpisodeShelf(shelfId, ::mapToEpisodeShelfRow)
      .asFlow()
      .mapToList(dispatcherProvider.databaseRead)
      .mapLatest { rows -> rows.map { it.asDomainModel(urlHydrator) } }
      .mapLatest { entries ->
        // Match the UI's key: libraryItemId + recentEpisodeId
        entries.dropDuplicates(shelfId) { it.libraryItem.id to it.recentEpisode.id }
      }
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
            .map { it.asDomainModel(urlHydrator) }
            .sortedBy { it.media.metadata.seriesSequence?.sequence }

          s.asDomainModel(sortedBooks)
        }.dropDuplicates(shelfId) { it.id }
      }
  }

  private fun readAuthors(shelfId: ShelfId): Flow<List<Author>> {
    return db.authorsQueries.selectForShelf(shelfId)
      .asFlow()
      .mapToList(dispatcherProvider.databaseRead)
      .mapLatest { authors ->
        authors.map { it.asDomainModel() }.dropDuplicates(shelfId) { it.id }
      }
  }
}

/**
 * The home UI keys its LazyRow items by entity id, so duplicate entities within a
 * single shelf are fatal. Duplicates should be impossible (the queries are scoped and
 * the join tables have composite PKs), so rather than let a data bug crash every app
 * launch, drop the duplicates and record a non-fatal to keep the bug visible.
 */
private inline fun <T, K> List<T>.dropDuplicates(shelfId: ShelfId, keySelector: (T) -> K): List<T> {
  val distinct = distinctBy(keySelector)
  if (distinct.size != size) {
    CrashReporter.record(DuplicateShelfEntitiesException(shelfId, size - distinct.size))
  }
  return distinct
}

class DuplicateShelfEntitiesException(
  shelfId: ShelfId,
  count: Int,
) : IllegalStateException("Shelf [$shelfId] emitted $count duplicate entities; duplicates were dropped")

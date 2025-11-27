package app.campfire.home.store.home

import app.campfire.CampfireDatabase
import app.campfire.account.api.TokenHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.LibraryId
import app.campfire.data.SeriesBookJoin
import app.campfire.data.ShelfJoin
import app.campfire.data.mapping.asDbModel
import app.campfire.home.api.model.Shelf
import app.campfire.home.mapping.asDbModel
import app.campfire.home.mapping.asDomainModel
import app.campfire.network.models.Author
import app.campfire.network.models.LibraryItemMinified
import app.campfire.network.models.MinifiedBookMetadata
import app.campfire.network.models.SeriesPersonalized
import app.campfire.network.models.Shelf as NetworkShelf
import app.cash.sqldelight.SuspendingTransactionWithoutReturn
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.mobilenativefoundation.store.store5.SourceOfTruth

class HomeSourceOfTruthFactory(
  private val db: CampfireDatabase,
  private val imageHydrator: TokenHydrator,
  private val dispatcherProvider: DispatcherProvider,
) {

  fun create(): SourceOfTruth<HomeStore.Key, List<NetworkShelf>, List<Shelf>> {
    return SourceOfTruth.of(
      reader = { key ->
        db.shelfQueries.select(key.libraryId)
          .asFlow()
          .mapToList(dispatcherProvider.databaseRead)
          .map { shelves ->
            shelves.map { it.asDomainModel() }
          }.map {
            // Store REALLY doesn't like empty lists as a state from the database
            // and can cause some odd emissions in certain circumstances and breaking
            // of the state machine as an empty list will appear as a valid return from
            // the SoT and then not load from network, etc.
            it.takeIf { it.isNotEmpty() }
          }
      },
      writer = { key, shelves ->
        withContext(dispatcherProvider.databaseWrite) {
          db.transaction {
            // Persist all the entities within a shelf
            shelves.forEachIndexed { index, shelf ->
              writeEntities(key.libraryId, shelf)

              // Persist shelf metadata
              val dbShelf = shelf.asDbModel(index, key.libraryId)
              db.shelfQueries.insert(dbShelf)
              writeEntityJoins(shelf)
            }
          }
        }
      },
      delete = { key ->
        withContext(dispatcherProvider.databaseWrite) {
          db.shelfQueries.delete(key.libraryId)
        }
      },
    )
  }

  private suspend fun SuspendingTransactionWithoutReturn.writeEntities(
    libraryId: LibraryId,
    shelf: NetworkShelf,
  ): Unit = when (shelf) {
    is NetworkShelf.BookShelf -> writeLibraryItems(shelf.entities)
    is NetworkShelf.EpisodeShelf -> writeLibraryItems(shelf.entities)
    is NetworkShelf.PodcastShelf -> writeLibraryItems(shelf.entities)
    is NetworkShelf.AuthorShelf -> writeAuthors(shelf.entities)
    is NetworkShelf.SeriesShelf -> writeSeries(libraryId, shelf.entities)
  }

  @Suppress("UnusedReceiverParameter")
  private suspend fun SuspendingTransactionWithoutReturn.writeEntityJoins(
    shelf: NetworkShelf,
  ) {
    val entityIds = when (shelf) {
      is NetworkShelf.BookShelf -> shelf.entities.map { it.id }
      is NetworkShelf.AuthorShelf -> shelf.entities.map { it.id }
      is NetworkShelf.EpisodeShelf -> shelf.entities.map { it.id }
      is NetworkShelf.PodcastShelf -> shelf.entities.map { it.id }
      is NetworkShelf.SeriesShelf -> shelf.entities.map { it.id }
    }

    entityIds.forEach { entityId ->
      db.shelfQueries.insertJoins(
        ShelfJoin(
          shelfId = shelf.id,
          entityId = entityId,
        ),
      )
    }
  }

  @Suppress("UnusedReceiverParameter")
  private suspend fun SuspendingTransactionWithoutReturn.writeLibraryItems(
    libraryItems: List<LibraryItemMinified<MinifiedBookMetadata>>,
  ) {
    libraryItems.forEach { item ->
      val libraryItem = item.asDbModel()
      val media = item.media.asDbModel(item.id)

      db.libraryItemsQueries.insertOrIgnore(libraryItem)
      db.mediaQueries.insertOrIgnore(media)
    }
  }

  @Suppress("UnusedReceiverParameter")
  private suspend fun SuspendingTransactionWithoutReturn.writeAuthors(
    authors: List<Author>,
  ) {
    val dbAuthors = authors.map { it.asDbModel(imageHydrator) }
    dbAuthors.forEach { author ->
      db.authorsQueries.insert(author)
    }
  }

  @Suppress("UnusedReceiverParameter")
  private suspend fun SuspendingTransactionWithoutReturn.writeSeries(
    libraryId: LibraryId,
    entities: List<SeriesPersonalized>,
  ) {
    entities.forEach { series ->
      // Upsert Series
      val exists = db.seriesQueries.existsById(series.id, libraryId)
        .awaitAsOneOrNull() != null
      if (exists) {
        db.seriesQueries.update(
          id = series.id,
          name = series.name,
          description = series.description,
          addedAt = series.addedAt,
          updatedAt = series.updatedAt,
          inProgress = series.inProgress == true,
          hasActiveBook = series.hasActiveBook == true,
          hideFromContinueListening = series.hideFromContinueListening == true,
          bookInProgressLastUpdate = series.bookInProgressLastUpdate,
          firstBookUnreadId = series.firstBookUnread?.id,
          libraryId = libraryId,
        )
      } else {
        db.seriesQueries.insertOrIgnore(series.asDbModel(libraryId))
      }

      // Insert the series books
      series.books?.forEachIndexed { index, book ->
        val libraryItem = book.asDbModel()
        val media = book.media.asDbModel(book.id, fallbackSeriesSequence = index + 1)

        // If these items exist, lets not overwrite their metadata
        db.libraryItemsQueries.insertOrIgnore(libraryItem)
        db.mediaQueries.insertOrIgnore(media)

        // Insert junction entry
        db.seriesBookJoinQueries.insert(
          SeriesBookJoin(
            seriesId = series.id,
            libraryItemId = book.id,
          ),
        )
      }
    }
  }
}

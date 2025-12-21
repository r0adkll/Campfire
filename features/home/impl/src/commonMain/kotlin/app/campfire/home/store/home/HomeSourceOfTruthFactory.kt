package app.campfire.home.store.home

import app.campfire.CampfireDatabase
import app.campfire.account.api.UrlHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.LibraryId
import app.campfire.core.model.ShelfType
import app.campfire.core.model.UserId
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
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.mobilenativefoundation.store.store5.SourceOfTruth

class HomeSourceOfTruthFactory(
  private val db: CampfireDatabase,
  private val imageHydrator: UrlHydrator,
  private val dispatcherProvider: DispatcherProvider,
) {

  fun create(): SourceOfTruth<HomeStore.Key, List<NetworkShelf>, List<Shelf>> {
    return SourceOfTruth.of(
      reader = { key ->
        db.shelfQueries.select(key.libraryId, key.userId)
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
        val currentShelves = withContext(dispatcherProvider.databaseRead) {
          db.shelfQueries.select(key.libraryId, key.userId).awaitAsList()
        }

        val currentJoins = withContext(dispatcherProvider.databaseRead) {
          db.shelfQueries.selectAllJoins().awaitAsList()
        }.groupBy { it.shelfId }

        val trashed = currentShelves.filter { shelves.none { shelf -> it.id == shelf.id } }

        withContext(dispatcherProvider.databaseWrite) {
          db.transaction {
            // Persist all the entities within a shelf
            shelves.forEachIndexed { index, shelf ->
              val isExisting = currentShelves.any { it.id == shelf.id }
              val isNew = currentShelves.none { it.id == shelf.id }

              // Either way we should always persist and update the entities in the shelves
              writeEntities(key.userId, key.libraryId, shelf)

              // Persist shelf metadata
              if (isNew) {
                // If the shelf is new, just insert it and its joins
                val dbShelf = shelf.asDbModel(
                  index = index,
                  libraryId = key.libraryId,
                  userId = key.userId,
                )
                db.shelfQueries.insert(dbShelf)
                writeEntityJoins(shelf)
              } else if (isExisting) {
                // If the shelf already exists, update the current shelf metadata
                // and its joins
                db.shelfQueries.update(
                  id = shelf.id,
                  label = shelf.label,
                  labelStringKey = shelf.labelStringKey,
                  total = shelf.total,
                  type = when (shelf) {
                    is NetworkShelf.AuthorShelf -> ShelfType.AUTHOR
                    is NetworkShelf.BookShelf -> ShelfType.BOOK
                    is NetworkShelf.EpisodeShelf -> ShelfType.EPISODE
                    is NetworkShelf.PodcastShelf -> ShelfType.PODCAST
                    is NetworkShelf.SeriesShelf -> ShelfType.SERIES
                  },
                  homeOrder = index,
                )

                val currentShelfJoins = currentJoins[shelf.id]
                  ?.map { it.entityId }
                  ?: emptyList()
                updateEntityJoins(shelf, currentShelfJoins)
              }
            }

            // Remove all shelves, and their joins, for shelfs that don't exist
            // in the returned network response
            trashed.forEach { shelf ->
              db.shelfQueries.deleteById(shelf.id)
            }
          }
        }
      },
      delete = { key ->
        withContext(dispatcherProvider.databaseWrite) {
          db.shelfQueries.delete(key.libraryId, key.userId)
        }
      },
    )
  }

  private suspend fun SuspendingTransactionWithoutReturn.writeEntities(
    userId: UserId,
    libraryId: LibraryId,
    shelf: NetworkShelf,
  ): Unit = when (shelf) {
    is NetworkShelf.BookShelf -> writeLibraryItems(shelf.entities)
    is NetworkShelf.EpisodeShelf -> writeLibraryItems(shelf.entities)
    is NetworkShelf.PodcastShelf -> writeLibraryItems(shelf.entities)
    is NetworkShelf.AuthorShelf -> writeAuthors(shelf.entities)
    is NetworkShelf.SeriesShelf -> writeSeries(userId, libraryId, shelf.entities)
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

    entityIds.forEachIndexed { index, entityId ->
      db.shelfQueries.insertJoins(
        ShelfJoin(
          shelfId = shelf.id,
          entityId = entityId,
          shelfOrder = index,
        ),
      )
    }
  }

  @Suppress("UnusedReceiverParameter")
  private suspend fun SuspendingTransactionWithoutReturn.updateEntityJoins(
    shelf: NetworkShelf,
    existingJoins: List<String>,
  ) {
    val entityIds = when (shelf) {
      is NetworkShelf.BookShelf -> shelf.entities.map { it.id }
      is NetworkShelf.AuthorShelf -> shelf.entities.map { it.id }
      is NetworkShelf.EpisodeShelf -> shelf.entities.map { it.id }
      is NetworkShelf.PodcastShelf -> shelf.entities.map { it.id }
      is NetworkShelf.SeriesShelf -> shelf.entities.map { it.id }
    }

    val trashed = existingJoins.filter { !entityIds.contains(it) }
    entityIds.forEachIndexed { index, entityId ->
      val isNew = !existingJoins.contains(entityId)

      if (isNew) {
        db.shelfQueries.insertJoins(
          ShelfJoin(
            shelfId = shelf.id,
            entityId = entityId,
            shelfOrder = index,
          ),
        )
      } else {
        db.shelfQueries.updateJoin(
          shelfId = shelf.id,
          entityId = entityId,
          shelfOrder = index,
        )
      }
    }

    db.shelfQueries.deleteJoins(shelf.id, trashed)
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
    userId: UserId,
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
        db.seriesQueries.insertOrIgnore(series.asDbModel(userId, libraryId))
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

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

// Hack to prevent other libraries/accounts from overwriting their shelves
fun NetworkShelf.uniqueId(
  userId: UserId,
  libraryId: LibraryId,
): String {
  return "${id}_${userId}_$libraryId"
}

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
        // Hack to prevent other libraries/accounts from overwriting their shelves
        fun NetworkShelf.uniqueId(): String = uniqueId(key.userId, key.libraryId)

        val currentShelves = withContext(dispatcherProvider.databaseRead) {
          db.shelfQueries.select(key.libraryId, key.userId).awaitAsList()
        }

        val trashed = currentShelves.filter { shelves.none { shelf -> it.id == shelf.uniqueId() } }

        withContext(dispatcherProvider.databaseWrite) {
          db.transaction {
            // Persist all the entities within a shelf
            shelves.forEachIndexed { index, shelf ->
              val isNew = currentShelves.none { it.id == shelf.uniqueId() }

              // Either way we should always persist and update the entities in the shelves
              writeEntities(key.userId, key.libraryId, shelf)

              // Persist shelf metadata
              if (isNew) {
                val dbShelf = shelf.asDbModel(
                  index = index,
                  libraryId = key.libraryId,
                  userId = key.userId,
                )
                db.shelfQueries.insert(dbShelf)
              } else {
                db.shelfQueries.update(
                  id = shelf.uniqueId(),
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
              }

              // Wipe-and-replace the join rows. The network response is the source
              // of truth and EpisodeShelf entries can repeat the same entityId with
              // different episodeIds — diffing on entityId alone (the prior approach)
              // collapsed those siblings.
              writeEntityJoins(key, shelf)
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
    is NetworkShelf.PodcastShelf -> writePodcastLibraryItems(shelf.entities)
    is NetworkShelf.EpisodeShelf -> writePodcastLibraryItems(shelf.entities)
    is NetworkShelf.AuthorShelf -> writeAuthors(shelf.entities)
    is NetworkShelf.SeriesShelf -> writeSeries(userId, libraryId, shelf.entities)
  }

  /**
   * One row per shelf entry in shelf order. For EpisodeShelf entries, [episodeId]
   * carries the specific recent episode the shelf is highlighting so reads can
   * join podcastEpisode back. For other shelf types it's the empty string (the
   * shelfJoin column's default), which keeps the composite PK collapsed to
   * (shelfId, entityId) for those types.
   */
  private data class JoinRow(val entityId: String, val episodeId: String)

  private fun NetworkShelf.joinRows(): List<JoinRow> = when (this) {
    is NetworkShelf.BookShelf -> entities.map { JoinRow(it.id, "") }
    is NetworkShelf.AuthorShelf -> entities.map { JoinRow(it.id, "") }
    is NetworkShelf.PodcastShelf -> entities.map { JoinRow(it.id, "") }
    is NetworkShelf.SeriesShelf -> entities.map { JoinRow(it.id, "") }
    is NetworkShelf.EpisodeShelf -> entities.mapNotNull { entry ->
      entry.recentEpisode?.id?.let { JoinRow(entry.id, it) }
    }
  }

  @Suppress("UnusedReceiverParameter")
  private suspend fun SuspendingTransactionWithoutReturn.writeEntityJoins(
    key: HomeStore.Key,
    shelf: NetworkShelf,
  ) {
    val shelfId = shelf.uniqueId(key.userId, key.libraryId)
    db.shelfQueries.deleteJoinsForShelf(shelfId)
    shelf.joinRows().forEachIndexed { index, row ->
      db.shelfQueries.insertJoins(
        ShelfJoin(
          shelfId = shelfId,
          entityId = row.entityId,
          shelfOrder = index,
          episodeId = row.episodeId,
        ),
      )
    }
  }

  @Suppress("UnusedReceiverParameter")
  private suspend fun SuspendingTransactionWithoutReturn.writeLibraryItems(
    libraryItems: List<LibraryItemMinified.Book>,
  ) {
    libraryItems.forEach { item ->
      val libraryItem = item.asDbModel()
      val media = item.media.asDbModel(item.id)

      db.libraryItemsQueries.insertOrIgnore(libraryItem)
      db.mediaQueries.insertOrIgnore(media)
    }
  }

  /**
   * Persist a list of podcast shelf entries: libraryItem row + podcastMedia row for each.
   * Entries that carry a [LibraryItemMinified.Podcast.recentEpisode] (the case for
   * `episodes-recently-added`-style shelves) also write the episode to [podcastEpisode]
   * so the read path can join it back via `shelfJoin.episodeId`.
   */
  @Suppress("UnusedReceiverParameter")
  private suspend fun SuspendingTransactionWithoutReturn.writePodcastLibraryItems(
    libraryItems: List<LibraryItemMinified.Podcast>,
  ) {
    libraryItems.forEach { item ->
      val libraryItem = item.asDbModel()
      val podcastMedia = item.media.asDbModel(item.id, imageHydrator)

      db.libraryItemsQueries.insertOrIgnore(libraryItem)
      db.podcastMediaQueries.insertOrIgnore(podcastMedia)

      item.recentEpisode?.let { episode ->
        val row = episode.asDbModel(
          libraryItemId = item.id,
          podcastMediaId = podcastMedia.mediaId,
        )
        db.podcastEpisodeQueries.insertOrIgnore(row)
      }
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

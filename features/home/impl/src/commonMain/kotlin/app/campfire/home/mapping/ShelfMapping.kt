package app.campfire.home.mapping

import app.campfire.CampfireDatabase
import app.campfire.account.api.TokenHydrator
import app.campfire.core.model.LibraryId
import app.campfire.core.model.ShelfType
import app.campfire.data.Shelf as DbShelf
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.dao.LibraryItemDao
import app.campfire.data.mapping.model.mapToLibraryItem
import app.campfire.home.api.model.Shelf as DomainShelf
import app.campfire.network.models.Shelf as NetworkShelf
import app.cash.sqldelight.async.coroutines.awaitAsList

suspend fun DbShelf.asDomainModel(
  db: CampfireDatabase,
  tokenHydrator: TokenHydrator,
  libraryItemDao: LibraryItemDao,
): DomainShelf<*> {
  return DomainShelf(
    id = id,
    label = label,
    total = total,
    entities = when (type) {
      ShelfType.BOOK,
      ShelfType.EPISODE,
      ShelfType.PODCAST,
      -> {
        db.libraryItemsQueries.selectForShelf(id, ::mapToLibraryItem)
          .awaitAsList()
          .map { libraryItemDao.hydrateItem(it) } as List<*>
      }

      ShelfType.SERIES -> {
        val series = db.seriesQueries
          .selectByShelfId(id)
          .awaitAsList()
          .associateWith { s ->
            db.libraryItemsQueries
              .selectForSeries(s.id)
              .awaitAsList()
          }

        series.entries.map { (s, books) ->
          val sortedBooks = books
            .map { it.asDomainModel(tokenHydrator) }
            .sortedBy { it.media.metadata.seriesSequence?.sequence }
          s.asDomainModel(sortedBooks)
        }
      }

      ShelfType.AUTHOR -> {
        db.authorsQueries.selectForShelf(id)
          .awaitAsList()
          .map { it.asDomainModel() }
      }
    },
  )
}

fun NetworkShelf.asDbModel(libraryId: LibraryId): DbShelf {
  return DbShelf(
    id = id,
    libraryId = libraryId,
    label = label,
    labelStringKey = labelStringKey,
    total = total,
    type = when (this) {
      is NetworkShelf.AuthorShelf -> ShelfType.AUTHOR
      is NetworkShelf.BookShelf -> ShelfType.BOOK
      is NetworkShelf.EpisodeShelf -> ShelfType.EPISODE
      is NetworkShelf.PodcastShelf -> ShelfType.PODCAST
      is NetworkShelf.SeriesShelf -> ShelfType.SERIES
    },
  )
}

package app.campfire.libraries.filtering.store

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.coroutines.mapIfNotNull
import app.campfire.core.model.FilterData
import app.campfire.core.model.LibraryId
import app.campfire.core.time.FatherTime
import app.campfire.data.FilterData as DbFilterData
import app.campfire.network.models.FilterData as NetworkFilterData
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import org.mobilenativefoundation.store.store5.SourceOfTruth

class FilterSourceOfTruthFactory(
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
  private val fatherTime: FatherTime,
) {

  fun create() = SourceOfTruth.of<LibraryId, NetworkFilterData, FilterData>(
    reader = { libraryId ->
      db.filterDataQueries
        .getFilterData(libraryId)
        .asFlow()
        .mapToOneOrNull(dispatcherProvider.databaseRead)
        .mapIfNotNull { filterData ->
          val authors = db.filterDataQueries.getFilterAuthors(libraryId, ::mapToEntity).awaitAsList()
          val genres = db.filterDataQueries.getFilterGenres(libraryId).awaitAsList()
          val tags = db.filterDataQueries.getFilterTags(libraryId).awaitAsList()
          val series = db.filterDataQueries.getFilterSeries(libraryId, ::mapToEntity).awaitAsList()
          val narrators = db.filterDataQueries.getFilterNarrators(libraryId).awaitAsList()
          val languages = db.filterDataQueries.getFilterLanguages(libraryId).awaitAsList()
          val publishers = db.filterDataQueries.getFilterPublishers(libraryId).awaitAsList()
          val publishedDecades = db.filterDataQueries.getFilterPublishedDecades(libraryId).awaitAsList()
          FilterData(
            authors = authors,
            genres = genres,
            tags = tags,
            series = series,
            narrators = narrators,
            languages = languages,
            publishers = publishers,
            publishedDecades = publishedDecades,
            bookCount = filterData.bookCount,
            authorCount = filterData.authorCount,
            seriesCount = filterData.seriesCount,
            podcastCount = filterData.podcastCount,
            numIssues = filterData.numIssues,
          )
        }
    },
    writer = { libraryId, filterData: NetworkFilterData ->
      db.transaction {
        db.filterDataQueries.insertFilterData(
          DbFilterData(
            libraryId = libraryId,
            bookCount = filterData.bookCount,
            authorCount = filterData.authorCount,
            seriesCount = filterData.seriesCount,
            podcastCount = filterData.podcastCount,
            numIssues = filterData.numIssues,
            lastUpdated = fatherTime.nowInEpochMillis(),
          ),
        )

        db.filterDataQueries.clearFilteredData(libraryId)

        filterData.authors.forEach { author ->
          db.filterDataQueries.insertAuthors(author.id, author.name, libraryId)
        }

        filterData.genres.forEach { genre ->
          db.filterDataQueries.insertGenres(genre, libraryId)
        }

        filterData.tags.forEach { tag ->
          db.filterDataQueries.insertTags(tag, libraryId)
        }

        filterData.series.forEach { series ->
          db.filterDataQueries.insertSeries(series.id, series.name, libraryId)
        }

        filterData.narrators.forEach { narrator ->
          db.filterDataQueries.insertNarrators(narrator, libraryId)
        }

        filterData.languages.forEach { language ->
          db.filterDataQueries.insertLanguages(language, libraryId)
        }

        filterData.publishers.forEach { publisher ->
          db.filterDataQueries.insertPublishers(publisher, libraryId)
        }

        filterData.publishedDecades.forEach { publishedDecade ->
          db.filterDataQueries.insertPublishedDecades(publishedDecade, libraryId)
        }
      }
    },
    delete = { libraryId ->
      db.filterDataQueries.delete(libraryId)
    },
  )

  private fun mapToEntity(id: String, name: String) = FilterData.Entity(id, name)
}

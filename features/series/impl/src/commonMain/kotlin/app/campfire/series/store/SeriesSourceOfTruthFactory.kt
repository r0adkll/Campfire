package app.campfire.series.store

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryId
import app.campfire.core.session.UserSession
import app.campfire.core.session.serverUrl
import app.campfire.data.SeriesBookJoin
import app.campfire.data.mapping.asDbModel
import app.campfire.network.models.Series as NetworkSeries
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import org.mobilenativefoundation.store.store5.SourceOfTruth

internal class SeriesSourceOfTruthFactory(
  private val userSession: UserSession,
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
) {

  fun create() = SourceOfTruth.of(
    reader = { libraryId: LibraryId ->
      db.seriesQueries.selectByLibraryId(libraryId)
        .asFlow()
        .mapToList(dispatcherProvider.databaseRead)
        .mapLatest { series ->
          series.associateWith { s ->
            db.libraryItemsQueries
              .selectForSeries(s.id)
              .awaitAsList()
          }
        }
    },
    writer = { libraryId, series: List<NetworkSeries> ->
      bark { "Writing Series: $series" }
      withContext(dispatcherProvider.databaseWrite) {
        db.transaction {
          series.forEach { series ->
            // Insert Series
            db.seriesQueries.insert(series.asDbModel(libraryId))

            // Insert the series books
            series.books?.forEach { book ->
              val libraryItem = book.asDbModel(userSession.serverUrl)
              val media = book.media.asDbModel(book.id)

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
    },
    delete = { libraryId: LibraryId ->
      withContext(dispatcherProvider.databaseWrite) {
        db.seriesQueries.deleteForLibraryId(libraryId)
      }
    },
  )
}

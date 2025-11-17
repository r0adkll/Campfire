package app.campfire.series.store

import app.campfire.CampfireDatabase
import app.campfire.account.api.TokenHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.LibraryId
import app.campfire.core.session.UserSession
import app.campfire.core.session.serverUrl
import app.campfire.core.util.runIfNotNull
import app.campfire.data.SeriesBookJoin
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDomainModel
import app.campfire.network.models.Series as NetworkSeries
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import org.mobilenativefoundation.store.store5.SourceOfTruth

internal class SeriesSourceOfTruthFactory(
  private val userSession: UserSession,
  private val db: CampfireDatabase,
  private val tokenHydrator: TokenHydrator,
  private val dispatcherProvider: DispatcherProvider,
) {

  @OptIn(ExperimentalCoroutinesApi::class)
  fun create() = SourceOfTruth.of(
    reader = { libraryId: LibraryId ->
      db.seriesQueries.selectByLibraryId(libraryId)
        .asFlow()
        .mapToList(dispatcherProvider.databaseRead)
        .mapLatest { series ->
          series.map { dbSeries ->
            val books = db.libraryItemsQueries
              .selectForSeries(dbSeries.id)
              .awaitAsList()
              .map { it.asDomainModel(tokenHydrator) }
              .sortedBy { it.media.metadata.seriesSequence?.sequence }

            dbSeries.asDomainModel(
              books = books,
            )
          }
        }
    },
    writer = { libraryId, series: List<NetworkSeries> ->
      withContext(dispatcherProvider.databaseWrite) {
        db.transaction {
          series.forEach { series ->
            // Insert Series
            db.seriesQueries.insertOrIgnore(series.asDbModel(libraryId))

            // Insert the series books
            series.books?.forEachIndexed { index, book ->
              val libraryItem = book.asDbModel(userSession.serverUrl)
              val media = book.media.asDbModel(book.id, fallbackSeriesSequence = index + 1)

              // If these items exist, lets not overwrite their metadata
              db.libraryItemsQueries.insertOrIgnore(libraryItem)
              db.mediaQueries.insertOrIgnore(media)

              // Make sure we keep our item series sequence up to date
              runIfNotNull(
                media.metadata_series_id,
                media.metadata_series_name,
                media.metadata_series_sequence,
              ) { id, name, sequence ->
                db.mediaQueries.updateSeriesSequence(id, name, sequence, book.id)
              }

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

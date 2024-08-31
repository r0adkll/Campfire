package app.campfire.series

import app.campfire.network.models.Series as NetworkSeries
import app.campfire.CampfireDatabase
import app.campfire.account.api.CoverImageHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryId
import app.campfire.core.model.Series
import app.campfire.core.session.UserSession
import app.campfire.data.SeriesBookJoin
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDbModels
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.asFetcherResult
import app.campfire.network.AudioBookShelfApi
import app.campfire.series.api.SeriesRepository
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.StoreBuilder
import org.mobilenativefoundation.store.store5.StoreReadRequest

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class StoreSeriesRepository(
  private val userSession: UserSession,
  private val api: AudioBookShelfApi,
  private val db: CampfireDatabase,
  private val coverImageHydrator: CoverImageHydrator,
  private val dispatcherProvider: DispatcherProvider,
) : SeriesRepository {

  private val serverUrl by lazy {
    userSession.serverUrl
      ?: throw IllegalStateException("Only logged-in users can fetch the list of series")
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private val seriesStore = StoreBuilder.from(
    fetcher = Fetcher.ofResult { libraryId: LibraryId -> api.getSeries(libraryId).asFetcherResult() },
    sourceOfTruth = SourceOfTruth.of(
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
        withContext(dispatcherProvider.databaseWrite) {
          db.transaction {
            series.forEach { series ->
              // Insert Series
              db.seriesQueries.insert(series.asDbModel(libraryId))

              // Insert the series books
              series.books?.forEach { book ->
                val (libraryItem, media) = book.asDbModels(serverUrl)
                db.libraryItemsQueries.insert(libraryItem)
                db.mediaQueries.insert(media)

                // Insert junction entry
                db.seriesBookJoinQueries.insert(
                  SeriesBookJoin(
                    seriesId = series.id,
                    libraryItemId = book.id,
                  )
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
  ).build()

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeAllSeries(): Flow<List<Series>> {
    return db.usersQueries.selectForServer(serverUrl)
      .asFlow()
      .mapToOne(dispatcherProvider.databaseRead)
      .flatMapLatest { user ->
        seriesStore.stream(StoreReadRequest.cached(user.selectedLibraryId, refresh = true))
          .mapNotNull { response ->
            response.dataOrNull()?.let { series ->
              series.entries.map { (s, books) ->
                s.asDomainModel(books.map { it.asDomainModel(coverImageHydrator) })
              }
            }
          }
      }
  }
}

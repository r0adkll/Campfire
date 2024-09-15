package app.campfire.series

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
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.asFetcherResult
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.models.Series as NetworkSeries
import app.campfire.account.api.UserRepository
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.SeriesId
import app.campfire.series.api.SeriesRepository
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
  private val userRepository: UserRepository,
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
                val libraryItem = book.asDbModel(serverUrl)
                val media = book.media.asDbModel(book.id)
                db.libraryItemsQueries.insert(libraryItem)
                db.mediaQueries.insert(media)

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
    ),
  ).build()

  data class SeriesItems(
    val libraryId: LibraryId,
    val seriesId: SeriesId,
  )

  @OptIn(ExperimentalEncodingApi::class)
  private val libraryItemStore = StoreBuilder.from(
    fetcher = Fetcher.ofResult { s: SeriesItems ->
      val encodedSeriesId = Base64.encode(s.seriesId.encodeToByteArray())
      api.getLibraryItems(s.libraryId, "series.${encodedSeriesId}").asFetcherResult()
    },
    sourceOfTruth = SourceOfTruth.of(
      reader = { s: SeriesItems ->
        db.libraryItemsQueries
          .selectForSeries(s.seriesId)
          .asFlow()
          .mapToList(dispatcherProvider.databaseRead)
          .mapNotNull { selectForSeries ->
            selectForSeries
              .map { it.asDomainModel(coverImageHydrator) }
              .takeIf { it.isNotEmpty() }
          }
      },
      writer = { s, items ->
        withContext(dispatcherProvider.databaseWrite) {
          db.transaction {
            items.forEach { item ->
              val libraryItem = item.asDbModel(serverUrl)
              val media = item.media.asDbModel(item.id)
              db.libraryItemsQueries.insert(libraryItem)
              db.mediaQueries.insert(media)

              // Insert junction entry
              db.seriesBookJoinQueries.insert(
                SeriesBookJoin(
                  seriesId = s.seriesId,
                  libraryItemId = item.id,
                ),
              )
            }
          }
        }
      },
    )
  ).build()

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeAllSeries(): Flow<List<Series>> {
    return userRepository.observeCurrentUser()
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

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeSeriesLibraryItems(seriesId: String): Flow<List<LibraryItem>> {
    return userRepository.observeCurrentUser()
      .flatMapLatest { user ->
        libraryItemStore.stream(
          StoreReadRequest.cached(
            SeriesItems(user.selectedLibraryId, seriesId),
            refresh = true,
          ),
        ).mapNotNull { response ->
          bark { "Series Library Item Response: $response" }
          response.dataOrNull()
        }
      }
  }
}

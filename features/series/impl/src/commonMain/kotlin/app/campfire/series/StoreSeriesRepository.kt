package app.campfire.series

import app.campfire.CampfireDatabase
import app.campfire.account.api.TokenHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.with
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Series
import app.campfire.core.model.SeriesId
import app.campfire.core.session.UserSession
import app.campfire.core.session.serverUrl
import app.campfire.core.util.runIfNotNull
import app.campfire.data.SeriesBookJoin
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.asFetcherResult
import app.campfire.data.mapping.store.debugLogging
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.models.LibraryItemFilter
import app.campfire.network.models.LibraryItemMinified
import app.campfire.network.models.MinifiedBookMetadata
import app.campfire.series.api.SeriesRepository
import app.campfire.series.store.SeriesStore
import app.campfire.user.api.UserRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.MemoryPolicy
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.StoreBuilder
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store.store5.StoreReadResponseOrigin

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class StoreSeriesRepository(
  private val userSession: UserSession,
  private val api: AudioBookShelfApi,
  private val db: CampfireDatabase,
  private val userRepository: UserRepository,
  private val tokenHydrator: TokenHydrator,
  private val dispatcherProvider: DispatcherProvider,
  private val seriesStoreFactory: SeriesStore.Factory,
) : SeriesRepository {

  private val serverUrl by lazy {
    userSession.serverUrl
      ?: throw IllegalStateException("Only logged-in users can fetch the list of series")
  }

  private val seriesStore by lazy { seriesStoreFactory.create() }

  data class SeriesItems(
    val libraryId: LibraryId,
    val seriesId: SeriesId,
  )

  data class SeriesNetworkResult(
    val series: app.campfire.network.models.Series,
    val books: List<LibraryItemMinified<MinifiedBookMetadata>>,
  )

  @Deprecated("This store interface needs to be extracted and updated to account for the APIs eccentricities")
  @OptIn(ExperimentalEncodingApi::class)
  private val libraryItemStore = StoreBuilder.from(
    fetcher = Fetcher.ofResult { s: SeriesItems ->
      withContext(dispatcherProvider.io) {
        val series = async { api.getSeriesById(s.libraryId, s.seriesId) }
        val books = async {
          api.getLibraryItemsMinified(
            libraryId = s.libraryId,
            filter = LibraryItemFilter(
              group = "series",
              value = s.seriesId,
            ),
          )
        }

        val seriesResult = series.await()
        val seriesBooksResult = books.await()

        seriesResult.with(seriesBooksResult) { series, books ->
          SeriesNetworkResult(series, books.data)
        }.asFetcherResult()
      }
    },
    sourceOfTruth = SourceOfTruth.of(
      reader = { s: SeriesItems ->
        db.libraryItemsQueries
          .selectForSeries(s.seriesId)
          .asFlow()
          .mapToList(dispatcherProvider.databaseRead)
          .mapNotNull { selectForSeries ->
            selectForSeries
              .map { it.asDomainModel(tokenHydrator) }
              .takeIf { it.isNotEmpty() }
          }
      },
      writer = { s, networkResult ->
        withContext(dispatcherProvider.databaseWrite) {
          db.transaction {
            // Insert the series first,
            val series = networkResult.series.asDbModel(s.libraryId)
            db.seriesQueries.insert(series)

            // Insert the books
            networkResult.books.forEach { item ->
              // TODO: Update when https://github.com/advplyr/audiobookshelf/pull/3945 is merged
//              libraryItemDao.insert(
//                item = item,
//                asTransaction = false,
//              )

              val libraryItem = item.asDbModel(serverUrl)
              val media = item.media.asDbModel(item.id)
              db.libraryItemsQueries.insertOrIgnore(libraryItem)
              db.mediaQueries.insertOrIgnore(media)

              // Make sure we keep our item series sequence up to date
              runIfNotNull(
                media.metadata_series_id,
                media.metadata_series_name,
                media.metadata_series_sequence,
              ) { id, name, sequence ->
                db.mediaQueries.updateSeriesSequence(id, name, sequence, item.id)
              }

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
    ),
  ).cachePolicy(
    MemoryPolicy.builder<SeriesItems, List<LibraryItem>>()
      .setExpireAfterAccess(5.minutes)
      .build(),
  ).build()

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeAllSeries(): Flow<List<Series>> {
    return userRepository.observeCurrentUser()
      .flatMapLatest { user ->
        seriesStore.stream(StoreReadRequest.cached(user.selectedLibraryId, refresh = true))
          .debugLogging("Series")
          .filterNot { it is StoreReadResponse.Loading || it is StoreReadResponse.NoNewData }
          .mapNotNull { response ->
            response.dataOrNull()?.let { series ->
              // If the response is empty, but was from the SoT, then lets just return null and wait
              // for the network request.
              if (series.isEmpty() && response.origin == StoreReadResponseOrigin.SourceOfTruth) {
                return@mapNotNull null
              }

              series
            }
          }
      }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeSeriesLibraryItems(seriesId: String): Flow<List<LibraryItem>> {
    return userRepository.observeCurrentUser()
      .flatMapLatest { user ->
        val request = StoreReadRequest.cached(
          SeriesItems(user.selectedLibraryId, seriesId),
          refresh = true,
        )
        libraryItemStore.stream(request)
          .debugLogging("LibraryItemStore::series")
          .mapNotNull { response ->
            response.dataOrNull()
          }.mapLatest { items ->
            items.sortedBy { it.media.metadata.seriesSequence?.sequence }
          }
      }
  }
}

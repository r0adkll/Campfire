package app.campfire.series

import app.campfire.CampfireDatabase
import app.campfire.account.api.TokenHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Series
import app.campfire.core.model.SeriesId
import app.campfire.core.session.UserSession
import app.campfire.core.session.serverUrl
import app.campfire.data.SeriesBookJoin
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.asFetcherResult
import app.campfire.data.mapping.dao.LibraryItemDao
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.models.Series as NetworkSeries
import app.campfire.series.api.SeriesRepository
import app.campfire.user.api.UserRepository
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.StoreBuilder
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class StoreSeriesRepository(
  private val userSession: UserSession,
  private val api: AudioBookShelfApi,
  private val db: CampfireDatabase,
  private val libraryItemDao: LibraryItemDao,
  private val userRepository: UserRepository,
  private val tokenHydrator: TokenHydrator,
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
      api.getLibraryItemsMinified(s.libraryId, "series.$encodedSeriesId").asFetcherResult()
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
      writer = { s, items ->
        withContext(dispatcherProvider.databaseWrite) {
          db.transaction {
            items.forEach { item ->
              // TODO: Update when https://github.com/advplyr/audiobookshelf/pull/3945 is merged
//              libraryItemDao.insert(
//                item = item,
//                asTransaction = false,
//              )

              val libraryItem = item.asDbModel(serverUrl)
              val media = item.media.asDbModel(item.id)
              db.libraryItemsQueries.insertOrIgnore(libraryItem)
              db.mediaQueries.insertOrIgnore(media)

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
  ).build()

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeAllSeries(): Flow<List<Series>> {
    return userRepository.observeCurrentUser()
      .flatMapLatest { user ->
        seriesStore.stream(StoreReadRequest.cached(user.selectedLibraryId, refresh = true))
          .filterNot { it is StoreReadResponse.Loading || it is StoreReadResponse.NoNewData }
          .mapNotNull { response ->
            response.dataOrNull()?.let { series ->
              series.entries.map { (s, books) ->
                val sortedBooks = books
                  .map { it.asDomainModel(tokenHydrator) }
                  .sortedBy { it.media.metadata.seriesSequence?.sequence }
                s.asDomainModel(sortedBooks)
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
          response.dataOrNull()
        }.mapLatest { items ->
          items.sortedBy { it.media.metadata.seriesSequence?.sequence }
        }
      }
  }
}

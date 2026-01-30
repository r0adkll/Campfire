package app.campfire.series.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import app.campfire.CampfireDatabase
import app.campfire.account.api.UrlHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.Series
import app.campfire.core.model.User
import app.campfire.data.mapping.asDomainModel
import app.campfire.db.paging.QueryPagingSource
import app.cash.sqldelight.async.coroutines.awaitAsList
import me.tatarka.inject.annotations.Inject

@Inject
class SeriesPagerFactory(
  private val remoteMediatorFactory: SeriesRemoteMediatorFactory,
  private val db: CampfireDatabase,
  private val urlHydrator: UrlHydrator,
  private val dispatcherProvider: DispatcherProvider,
) {

  @OptIn(ExperimentalPagingApi::class)
  fun create(
    user: User,
    input: SeriesPagingInput,
  ): Pager<Int, Series> {
    return Pager(
      config = PagingConfig(
        pageSize = DEFAULT_PAGE_SIZE,
        initialLoadSize = DEFAULT_PAGE_SIZE,
      ),
      remoteMediator = remoteMediatorFactory(user, input),
    ) {
      QueryPagingSource(
        countQuery = db.seriesPageQueries.count(
          userId = user.id,
          libraryId = user.selectedLibraryId,
          input = input.databaseKey,
        ),
        transacter = db.seriesPageQueries,
        context = dispatcherProvider.databaseRead,
        queryProvider = { limit: Long, offset: Long ->
          db.seriesPageQueries.selectSeriesWithLimitAndOffset(
            userId = user.id,
            libraryId = user.selectedLibraryId,
            input = input.databaseKey,
            limit = limit,
            offset = offset,
          )
        },
        mapper = { dbSeries ->
          val books = db.libraryItemsQueries
            .selectForSeries(dbSeries.id)
            .awaitAsList()
            .map { it.asDomainModel(urlHydrator) }
            .sortedBy { it.media.metadata.seriesSequence?.sequence }

          dbSeries.asDomainModel(
            books = books,
          )
        },
      )
    }
  }

  companion object {
    private const val DEFAULT_PAGE_SIZE = 50
  }
}

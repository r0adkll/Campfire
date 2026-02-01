package app.campfire.db.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import app.cash.sqldelight.Query
import app.cash.sqldelight.SuspendingTransacter
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.withContext

@Suppress("FunctionName")
fun <DomainType : Any, DatabaseType : Any> QueryPagingSource(
  countQuery: Query<Long>,
  transacter: SuspendingTransacter,
  context: CoroutineContext,
  queryProvider: (limit: Long, offset: Long) -> Query<DatabaseType>,
  mapper: suspend (DatabaseType) -> DomainType,
  initialOffset: Long = 0,
  /**
   * A separate query from [queryProvider] to listen in on instead. This is useful
   * if there is only a subset of the query you want to listen to changes for.
   */
  queryObserverProvider: ((limit: Long, offset: Long) -> Query<*>)? = null,
): PagingSource<Int, DomainType> = OffsetQueryPagingSource(
  { limit, offset -> queryProvider(limit.toLong(), offset.toLong()) },
  queryObserverProvider?.let {
    { limit, offset -> it(limit.toLong(), offset.toLong()) }
  },
  mapper,
  countQuery,
  transacter,
  context,
  initialOffset.toInt(),
) as PagingSource<Int, DomainType>

internal class OffsetQueryPagingSource<DomainType : Any, DatabaseType : Any>(
  private val queryProvider: (limit: Int, offset: Int) -> Query<DatabaseType>,
  private val queryObserverProvider: ((limit: Int, offset: Int) -> Query<*>)?,
  private val mapper: suspend (DatabaseType) -> DomainType,
  private val countQuery: Query<Long>,
  private val transacter: SuspendingTransacter,
  private val context: CoroutineContext,
  private val initialOffset: Int,
) : QueryPagingSource<Int, DomainType, DatabaseType>() {

  override val jumpingSupported get() = true

  override suspend fun load(
    params: LoadParams<Int>,
  ): LoadResult<Int, DomainType> = withContext(context) {
    val key = params.key ?: initialOffset
    val limit = when (params) {
      is LoadParams.Prepend -> minOf(key, params.loadSize)
      else -> params.loadSize
    }

    val loadResult = transacter.transactionWithResult {
      val count = countQuery.executeAsOne().toInt()
      val offset = when (params) {
        is LoadParams.Prepend -> maxOf(0, key - params.loadSize)
        is LoadParams.Append -> key
        is LoadParams.Refresh -> if (key >= count - params.loadSize) maxOf(0, count - params.loadSize) else key
      }
      val databaseData = queryProvider(limit, offset)
        .also {
          currentQuery = if (queryObserverProvider != null) {
            queryObserverProvider(limit, offset)
          } else {
            it
          }
        }
        .executeAsList()

      val domainData = databaseData.map {
        mapper(it)
      }

      val nextPosToLoad = offset + domainData.size
      LoadResult.Page(
        data = domainData,
        prevKey = offset.takeIf { it > 0 && domainData.isNotEmpty() },
        nextKey = nextPosToLoad.takeIf { domainData.isNotEmpty() && domainData.size >= limit && it < count },
        itemsBefore = offset,
        itemsAfter = maxOf(0, count - nextPosToLoad),
      )
    }

    (if (invalid) LoadResult.Invalid() else loadResult)
  }

  override fun getRefreshKey(state: PagingState<Int, DomainType>): Int? {
    return state.anchorPosition?.let { maxOf(0, it - (state.config.initialLoadSize / 2)) }
  }
}

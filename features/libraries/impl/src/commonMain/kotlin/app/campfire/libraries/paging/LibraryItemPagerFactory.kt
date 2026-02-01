package app.campfire.libraries.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.User
import app.campfire.data.mapping.dao.LibraryItemDao
import app.campfire.data.mapping.model.mapToLibraryItemWithProgress
import app.campfire.db.paging.QueryPagingSource
import me.tatarka.inject.annotations.Inject

@Inject
class LibraryItemPagerFactory(
  private val remoteMediatorFactory: LibraryItemRemoteMediatorFactory,
  private val db: CampfireDatabase,
  private val libraryItemDao: LibraryItemDao,
  private val dispatcherProvider: DispatcherProvider,
) {

  @OptIn(ExperimentalPagingApi::class)
  fun create(
    user: User,
    input: LibraryItemPagingInput,
  ): Pager<Int, LibraryItem> {
    return Pager(
      config = PagingConfig(
        pageSize = DEFAULT_PAGE_SIZE,
        initialLoadSize = DEFAULT_PAGE_SIZE,
        enablePlaceholders = false,
      ),
      remoteMediator = remoteMediatorFactory(user, input),
    ) {
      QueryPagingSource(
        countQuery = db.libraryItemPageQueries.count(
          userId = user.id,
          libraryId = user.selectedLibraryId,
          input = input.databaseKey,
        ),
        transacter = db.libraryItemPageQueries,
        context = dispatcherProvider.databaseRead,
        queryProvider = { limit: Long, offset: Long ->
          db.libraryItemPageQueries.selectLibraryItemsWithLimitOffset(
            userId = user.id,
            libraryId = user.selectedLibraryId,
            input = input.databaseKey,
            limit = limit,
            offset = offset,
            ::mapToLibraryItemWithProgress,
          )
        },
        queryObserverProvider = { limit: Long, offset: Long ->
          /*
           * Provide an alternative observable sqldelight [Query] here to prevent unnecessary churn
           * when opening an item detail. This is because the detail screen downloads a fully expanded library
           * item object and updates the underlying datastore. Due to the that table being included in the
           * query above it will cause the PagingSource to invalidate and create a ton of visual churn
           * which is an un-ideal user experience.
           *
           * The data from this query is never actually requested or used and instead is used as an
           * observability trigger against the remote page keys in the database. These only change with the
           * paging setup itself.
           */
          db.libraryItemPageQueries.selectLibraryItemsPagesWithLimitOffset(
            userId = user.id,
            libraryId = user.selectedLibraryId,
            input = input.databaseKey,
            limit = limit,
            offset = offset,
          )
        },
        mapper = {
          libraryItemDao.hydrateItem(it)
        },
      )
    }
  }

  companion object {
    private const val DEFAULT_PAGE_SIZE = 50
  }
}

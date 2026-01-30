package app.campfire.author.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.Author
import app.campfire.core.model.User
import app.campfire.data.mapping.asDomainModel
import app.campfire.db.paging.QueryPagingSource
import me.tatarka.inject.annotations.Inject

@Inject
class AuthorsPagerFactory(
  private val remoteMediatorFactory: AuthorsRemoteMediatorFactory,
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
) {

  @OptIn(ExperimentalPagingApi::class)
  fun create(
    user: User,
    input: AuthorsPagingInput,
  ): Pager<Int, Author> {
    return Pager(
      config = PagingConfig(
        pageSize = DEFAULT_PAGE_SIZE,
        initialLoadSize = DEFAULT_PAGE_SIZE,
      ),
      remoteMediator = remoteMediatorFactory(user, input),
    ) {
      QueryPagingSource(
        countQuery = db.authorsPageQueries.count(
          userId = user.id,
          libraryId = user.selectedLibraryId,
          input = input.databaseKey,
        ),
        transacter = db.authorsPageQueries,
        context = dispatcherProvider.databaseRead,
        queryProvider = { limit: Long, offset: Long ->
          db.authorsPageQueries.selectAuthorsWithLimitOffset(
            userId = user.id,
            libraryId = user.selectedLibraryId,
            input = input.databaseKey,
            limit = limit,
            offset = offset,
          )
        },
        mapper = {
          it.asDomainModel()
        },
      )
    }
  }

  companion object {
    private const val DEFAULT_PAGE_SIZE = 50
  }
}

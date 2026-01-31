package app.campfire.author

import app.campfire.CampfireDatabase
import app.campfire.author.api.AuthorPager
import app.campfire.author.api.AuthorRepository
import app.campfire.author.paging.AuthorsPagerFactory
import app.campfire.author.paging.AuthorsPagingInput
import app.campfire.author.store.AuthorDetailStore
import app.campfire.author.store.LibraryAuthorStore
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.Author
import app.campfire.core.model.User
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.SortDirection
import app.campfire.data.mapping.asDomainModel
import app.campfire.user.api.UserRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.StoreReadRequest

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class StoreAuthorRepository(
  private val userRepository: UserRepository,
  private val libraryAuthorStore: LibraryAuthorStore,
  private val authorsPagerFactory: AuthorsPagerFactory,
  private val authorDetailStore: AuthorDetailStore,
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
) : AuthorRepository {

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeAuthors(): Flow<List<Author>> {
    return userRepository.observeCurrentUser()
      .flatMapLatest { user ->
        libraryAuthorStore.store
          .stream(StoreReadRequest.cached(user.selectedLibraryId, refresh = true))
          .mapNotNull { response ->
            response.dataOrNull()?.let { dbAuthors ->
              dbAuthors
                .map { author ->
                  author.asDomainModel()
                }
                .sortedBy { author ->
                  author.name
                }
            }
          }
      }
  }

  override fun createAuthorsPager(
    user: User,
    sortMode: ContentSortMode,
    sortDirection: SortDirection,
  ): AuthorPager {
    val input = AuthorsPagingInput(sortMode, sortDirection)
    return authorsPagerFactory.create(user, input)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeFilteredAuthorsCount(sortMode: ContentSortMode, sortDirection: SortDirection): Flow<Int?> {
    return userRepository.observeCurrentUser()
      .flatMapLatest { user ->
        val input = AuthorsPagingInput(sortMode, sortDirection)
        db.authorsPageQueries
          .selectOldestPage(
            input = input.databaseKey,
            userId = user.id,
            libraryId = user.selectedLibraryId,
          )
          .asFlow()
          .mapToOneOrNull(dispatcherProvider.databaseRead)
          .map { it?.total }
      }
  }

  override fun observeAuthor(authorId: String): Flow<Author> {
    return authorDetailStore.store
      .stream(StoreReadRequest.cached(authorId, refresh = true))
      .mapNotNull { response ->
        response.dataOrNull()
      }
  }
}

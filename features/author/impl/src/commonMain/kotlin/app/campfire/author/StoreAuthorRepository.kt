package app.campfire.author

import app.campfire.author.api.AuthorRepository
import app.campfire.author.store.AuthorDetailStore
import app.campfire.author.store.LibraryAuthorStore
import app.campfire.core.di.UserScope
import app.campfire.core.model.Author
import app.campfire.data.mapping.asDomainModel
import app.campfire.user.api.UserRepository
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import org.mobilenativefoundation.store.store5.StoreReadRequest

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class StoreAuthorRepository(
  private val userRepository: UserRepository,
  private val libraryAuthorStore: LibraryAuthorStore,
  private val authorDetailStore: AuthorDetailStore,
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

  override fun observeAuthor(authorId: String): Flow<Author> {
    return authorDetailStore.store
      .stream(StoreReadRequest.cached(authorId, refresh = true))
      .mapNotNull { response ->
        response.dataOrNull()
      }
  }
}

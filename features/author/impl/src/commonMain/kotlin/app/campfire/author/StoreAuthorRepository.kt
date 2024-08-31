package app.campfire.author

import app.campfire.CampfireDatabase
import app.campfire.account.api.CoverImageHydrator
import app.campfire.author.api.AuthorRepository
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.Author
import app.campfire.core.model.LibraryId
import app.campfire.core.session.UserSession
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.asFetcherResult
import app.campfire.network.AudioBookShelfApi
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
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
class StoreAuthorRepository(
  private val userSession: UserSession,
  private val api: AudioBookShelfApi,
  private val db: CampfireDatabase,
  private val imageHydrator: CoverImageHydrator,
  private val dispatcherProvider: DispatcherProvider,
) : AuthorRepository {

  private val authorStore = StoreBuilder.from(
    fetcher = Fetcher.ofResult { libraryId: LibraryId -> api.getAuthors(libraryId).asFetcherResult() },
    sourceOfTruth = SourceOfTruth.of(
      reader = { libraryId: LibraryId ->
        db.authorsQueries.selectForLibrary(libraryId)
          .asFlow()
          .mapToList(dispatcherProvider.databaseRead)
      },
      writer = { libraryId: LibraryId, authors ->
        val dbAuthors = authors.map { it.asDbModel(libraryId, imageHydrator) }
        withContext(dispatcherProvider.databaseWrite) {
          db.transaction {
            dbAuthors.forEach { db.authorsQueries.insert(it) }
          }
        }
      },
      delete = { libraryId: LibraryId ->
        withContext(dispatcherProvider.databaseWrite) {
          db.authorsQueries.deleteForLibrary(libraryId)
        }
      }
    )
  ).build()

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeAuthors(): Flow<List<Author>> {
    val serverUrl = userSession.serverUrl
      ?: throw IllegalStateException("Only logged in users can request authors")
    return db.usersQueries.selectForServer(serverUrl)
      .asFlow()
      .mapToOne(dispatcherProvider.databaseRead)
      .flatMapLatest { user ->
        authorStore.stream(StoreReadRequest.cached(user.selectedLibraryId, refresh = true))
          .mapNotNull { response ->
            response.dataOrNull()?.let { dbAuthors ->
              dbAuthors.map { author ->
                author.asDomainModel()
              }
            }
          }
      }
  }
}

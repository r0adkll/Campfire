package app.campfire.libraries

import app.campfire.CampfireDatabase
import app.campfire.account.api.TokenHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.Library
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.UserId
import app.campfire.core.session.UserSession
import app.campfire.core.session.serverUrl
import app.campfire.core.session.userId
import app.campfire.data.Library as DbLibrary
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.asFetcherResult
import app.campfire.data.mapping.model.LibraryItemWithMedia
import app.campfire.data.mapping.model.mapToLibraryItem
import app.campfire.libraries.api.LibraryRepository
import app.campfire.network.AudioBookShelfApi
import app.campfire.user.api.UserRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.MemoryPolicy
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.StoreBuilder
import org.mobilenativefoundation.store.store5.StoreReadRequest

@OptIn(ExperimentalCoroutinesApi::class)
@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class StoreLibraryRepository(
  private val userSession: UserSession,
  private val api: AudioBookShelfApi,
  private val db: CampfireDatabase,
  private val userRepository: UserRepository,
  private val tokenHydrator: TokenHydrator,
  private val dispatcherProvider: DispatcherProvider,
) : LibraryRepository {

  private val libraryItemStore = StoreBuilder
    .from(
      fetcher = Fetcher.ofResult { libraryId: LibraryId ->
        api.getLibraryItemsMinified(libraryId).asFetcherResult()
      },
      sourceOfTruth = SourceOfTruth.of(
        reader = { libraryId: LibraryId ->
          db.libraryItemsQueries.selectForLibrary(libraryId, ::mapToLibraryItem)
            .asFlow()
            .mapToList(dispatcherProvider.databaseRead)
        },
        writer = { _, data ->
          withContext(dispatcherProvider.databaseWrite) {
            db.transaction {
              data.forEach { item ->
                // TODO: Update when https://github.com/advplyr/audiobookshelf/pull/3945 is merged
//                libraryItemDao.insert(
//                  item = item,
//                  asTransaction = false,
//                )

                val libraryItem = item.asDbModel(userSession.serverUrl!!)
                val media = item.media.asDbModel(item.id)

                db.libraryItemsQueries.insertOrIgnore(libraryItem)
                db.mediaQueries.insertOrIgnore(media)
              }
            }
          }
        },
        delete = { libraryId: LibraryId ->
          withContext(dispatcherProvider.databaseWrite) {
            db.libraryItemsQueries.deleteForLibrary(libraryId)
          }
        },
      ),
    )
    .cachePolicy(
      MemoryPolicy.builder<LibraryId, List<LibraryItemWithMedia>>()
        .setMaxSize(10)
        .setExpireAfterAccess(15.minutes)
        .build(),
    )
    .build()

  data class SingleLibraryRequest(val userId: UserId, val libraryId: LibraryId)

  private val singleLibraryStore = StoreBuilder.from(
    fetcher = Fetcher.ofResult { request: SingleLibraryRequest ->
      api.getLibrary(request.libraryId).asFetcherResult()
    },
    sourceOfTruth = SourceOfTruth.of(
      reader = { request: SingleLibraryRequest ->
        db.librariesQueries.selectById(request.libraryId)
          .asFlow()
          .mapToOneOrNull(dispatcherProvider.databaseRead)
      },
      writer = { request: SingleLibraryRequest, data ->
        val libraryItem = data.asDbModel(request.userId)
        withContext(dispatcherProvider.databaseWrite) {
          db.librariesQueries.insert(libraryItem)
        }
      },
      delete = { request: SingleLibraryRequest ->
        withContext(dispatcherProvider.databaseWrite) {
          db.librariesQueries.deleteById(request.libraryId)
        }
      },
    ),
  ).build()

  private val allLibrariesStore = StoreBuilder.from(
    fetcher = Fetcher.ofResult { api.getAllLibraries().asFetcherResult() },
    sourceOfTruth = SourceOfTruth.of(
      reader = { userId: UserId ->
        db.librariesQueries.selectAll(userId)
          .asFlow()
          .mapToList(dispatcherProvider.databaseRead)
      },
      writer = { userId, data ->
        withContext(dispatcherProvider.databaseWrite) {
          db.transaction {
            data.forEach { library ->
              val libraryItem = library.asDbModel(userId)
              db.librariesQueries.insert(libraryItem)
            }
          }
        }
      },
      delete = { userId ->
        withContext(dispatcherProvider.databaseWrite) {
          db.librariesQueries.deleteAll(userId)
        }
      },
    ),
  ).cachePolicy(
    MemoryPolicy.builder<UserId, List<DbLibrary>>()
      .setMaxSize(15)
      .setExpireAfterAccess(30.minutes)
      .build(),
  ).build()

  override fun observeCurrentLibrary(): Flow<Library> {
    return userRepository.observeCurrentUser()
      .flatMapLatest { user ->
        // Fetch the latest library based on the value in the User has selected in the database.
        // If the user changes libraries an active subscription to this flow should update the current library
        val request = SingleLibraryRequest(user.id, user.selectedLibraryId)
        singleLibraryStore
          .stream(StoreReadRequest.cached(request, refresh = true))
          .mapNotNull {
            it.dataOrNull()?.asDomainModel()
          }
      }
  }

  override fun observeAllLibraries(): Flow<List<Library>> {
    return userRepository.observeCurrentUser()
      .flatMapLatest { user ->
        allLibrariesStore
          .stream(StoreReadRequest.cached(user.id, refresh = true))
          .mapNotNull {
            it.dataOrNull()?.map { it.asDomainModel() }
          }
      }
  }

  override fun observeLibraryItems(): Flow<List<LibraryItem>> {
    return userRepository.observeCurrentUser()
      .flatMapLatest { user ->
        libraryItemStore
          .stream(StoreReadRequest.cached(user.selectedLibraryId, refresh = true))
          .mapNotNull {
            it.dataOrNull()?.map { it.asDomainModel(tokenHydrator) }
          }
      }
  }

  override suspend fun setCurrentLibrary(library: Library) {
    val userId = userSession.userId ?: return
    withContext(dispatcherProvider.databaseWrite) {
      db.usersQueries.updateSelectedLibrary(library.id, userId)
    }
  }
}

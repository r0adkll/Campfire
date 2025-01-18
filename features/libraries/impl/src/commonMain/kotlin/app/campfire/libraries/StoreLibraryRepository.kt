package app.campfire.libraries

import app.campfire.CampfireDatabase
import app.campfire.account.api.CoverImageHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.Library
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItem
import app.campfire.core.session.UserSession
import app.campfire.core.session.serverUrl
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.asFetcherResult
import app.campfire.data.mapping.model.LibraryItemWithMedia
import app.campfire.data.mapping.model.mapToLibraryItem
import app.campfire.libraries.api.LibraryRepository
import app.campfire.network.AudioBookShelfApi
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.MemoryPolicy
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.StoreBuilder
import org.mobilenativefoundation.store.store5.StoreReadRequest

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class StoreLibraryRepository(
  private val userSession: UserSession,
  private val api: AudioBookShelfApi,
  private val db: CampfireDatabase,
  private val coverImageHydrator: CoverImageHydrator,
  private val dispatcherProvider: DispatcherProvider,
) : LibraryRepository {

  private val libraryItemStore = StoreBuilder
    .from(
      fetcher = Fetcher.ofResult { libraryId: LibraryId ->
        api.getLibraryItems(libraryId).asFetcherResult()
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
                val libraryItem = item.asDbModel(userSession.serverUrl!!)
                val media = item.media.asDbModel(item.id)

                db.libraryItemsQueries.insert(libraryItem)
                db.mediaQueries.insert(media)
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

  private val singleLibraryStore = StoreBuilder.from(
    fetcher = Fetcher.ofResult { libraryId: LibraryId -> api.getLibrary(libraryId).asFetcherResult() },
    sourceOfTruth = SourceOfTruth.of(
      reader = { libraryId: LibraryId ->
        db.librariesQueries.selectById(libraryId)
          .asFlow()
          .mapToOneOrNull(dispatcherProvider.databaseRead)
      },
      writer = { _, data ->
        val libraryItem = data.asDbModel()
        withContext(dispatcherProvider.databaseWrite) {
          db.librariesQueries.insert(libraryItem)
        }
      },
      delete = { libraryId: LibraryId ->
        withContext(dispatcherProvider.databaseWrite) {
          db.librariesQueries.deleteById(libraryId)
        }
      },
    ),
  ).build()

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeCurrentLibrary(): Flow<Library> {
    val serverUrl = userSession.serverUrl
      ?: throw IllegalStateException("Only logged in users can request library items")
    // TODO: We should move this to a UserRepository where we can cache the current user for a given
    //  server url without having to pull from the DB everytime
    return db.usersQueries.selectForServer(serverUrl)
      .asFlow()
      .mapToOne(dispatcherProvider.databaseRead)
      .flatMapLatest { user ->
        // Fetch the latest library based on the value in the User has selected in the database.
        // If the user changes libraries an active subscription to this flow should update the current library
        singleLibraryStore
          .stream(StoreReadRequest.cached(user.selectedLibraryId, refresh = true))
          .mapNotNull {
            it.dataOrNull()?.asDomainModel()
          }
      }
  }

  override fun observeAllLibraries(): Flow<List<Library>> {
    TODO("Not yet implemented")
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeLibraryItems(): Flow<List<LibraryItem>> {
    val currentServerUrl = userSession.serverUrl
      ?: throw IllegalStateException("Only logged in users can request library items")
    return db.usersQueries.selectForServer(currentServerUrl)
      .asFlow()
      .mapToOne(dispatcherProvider.databaseRead)
      .flatMapLatest { user ->
        libraryItemStore
          .stream(StoreReadRequest.cached(user.selectedLibraryId, refresh = true))
          .mapNotNull {
            it.dataOrNull()?.map { it.asDomainModel(coverImageHydrator) }
          }
      }
  }
}

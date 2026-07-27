package app.campfire.libraries

import app.campfire.CampfireDatabase
import app.campfire.account.api.UrlHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.filter.ContentFilter
import app.campfire.core.model.Library
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.User
import app.campfire.core.model.UserId
import app.campfire.core.session.UserSession
import app.campfire.core.session.userId
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.SortDirection
import app.campfire.data.Library as DbLibrary
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.asFetcherResult
import app.campfire.data.mapping.dao.LibraryItemDao
import app.campfire.data.mapping.store.debugLogging
import app.campfire.libraries.api.AddPodcastContext
import app.campfire.libraries.api.LibraryFolder
import app.campfire.libraries.api.LibraryRepository
import app.campfire.libraries.api.paging.LibraryItemPager
import app.campfire.libraries.paging.LibraryItemPagerFactory
import app.campfire.libraries.paging.LibraryItemPagingInput
import app.campfire.libraries.paging.fetchAllLibraryItems
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.models.LibraryItemMinified
import app.campfire.user.api.UserRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
  private val libraryItemPagingFactory: LibraryItemPagerFactory,
  private val libraryItemDao: LibraryItemDao,
  private val urlHydrator: UrlHydrator,
  private val dispatcherProvider: DispatcherProvider,
) : LibraryRepository {

  data class SingleLibraryRequest(val userId: UserId, val libraryId: LibraryId)

  data class LibraryItemsRequest(
    val userId: UserId,
    val libraryId: LibraryId,
    val serverUrl: String,
  )

  private val singleLibraryStore = StoreBuilder.from(
    fetcher = Fetcher.ofResult { request: SingleLibraryRequest ->
      api.getLibrary(request.libraryId).asFetcherResult()
    },
    sourceOfTruth = SourceOfTruth.of(
      reader = { request: SingleLibraryRequest ->
        db.librariesQueries.selectById(request.libraryId)
          .asFlow()
          .mapToOneOrNull(dispatcherProvider.databaseRead)
          .map { it?.asDomainModel() }
      },
      writer = { request: SingleLibraryRequest, data ->
        val libraryItem = data.asDbModel(request.userId)
        withContext(dispatcherProvider.databaseWrite) {
          db.librariesQueries.update(
            id = request.libraryId,
            name = libraryItem.name,
            displayOrder = libraryItem.displayOrder,
            icon = libraryItem.icon,
            mediaType = libraryItem.mediaType,
            provider = libraryItem.provider,
            createdAt = libraryItem.createdAt,
            lastUpdate = libraryItem.lastUpdate,
            coverAspectRatio = libraryItem.coverAspectRatio,
            audiobooksOnly = libraryItem.audiobooksOnly,
          )
          db.librariesQueries.insert(libraryItem)
        }
      },
      delete = { request: SingleLibraryRequest ->
        withContext(dispatcherProvider.databaseWrite) {
          db.librariesQueries.deleteById(request.libraryId)
        }
      },
    ),
  )
    .cachePolicy(
      MemoryPolicy.MemoryPolicyBuilder<SingleLibraryRequest, Library>()
        .build(),
    )
    .build()

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
              db.librariesQueries.update(
                id = library.id,
                name = libraryItem.name,
                displayOrder = libraryItem.displayOrder,
                icon = libraryItem.icon,
                mediaType = libraryItem.mediaType,
                provider = libraryItem.provider,
                createdAt = libraryItem.createdAt,
                lastUpdate = libraryItem.lastUpdate,
                coverAspectRatio = libraryItem.coverAspectRatio,
                audiobooksOnly = libraryItem.audiobooksOnly,
              )
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

  private val currentLibraryItemsStore = StoreBuilder.from(
    fetcher = Fetcher.ofResult { request: LibraryItemsRequest ->
      fetchAllLibraryItems(api, request.libraryId).asFetcherResult()
    },
    sourceOfTruth = SourceOfTruth.of(
      reader = { request: LibraryItemsRequest ->
        db.libraryItemsQueries.selectAllForLibrary(request.libraryId)
          .asFlow()
          .mapToList(dispatcherProvider.databaseRead)
          .map { rows ->
            rows
              .map { libraryItemDao.hydratePagedItem(it) }
              .sortedBy { it.media.metadata.title?.lowercase() }
          }
      },
      writer = { request: LibraryItemsRequest, data ->
        withContext(dispatcherProvider.databaseWrite) {
          db.transaction {
            data.forEach { item ->
              when (item) {
                is LibraryItemMinified.Book -> {
                  db.libraryItemsQueries.insertOrIgnore(item.asDbModel(request.serverUrl))
                  db.mediaQueries.insertOrIgnore(item.media.asDbModel(item.id))
                }
                is LibraryItemMinified.Podcast -> {
                  db.libraryItemsQueries.insertOrIgnore(item.asDbModel(request.serverUrl))
                  db.podcastMediaQueries.insertOrIgnore(item.media.asDbModel(item.id, urlHydrator))
                }
              }
            }
          }
        }
      },
    ),
  ).cachePolicy(
    MemoryPolicy.builder<LibraryItemsRequest, List<LibraryItem>>()
      .setMaxSize(4)
      .setExpireAfterAccess(30.minutes)
      .build(),
  ).build()

  override fun observeCurrentLibrary(refresh: Boolean): Flow<Library> {
    return userRepository.userFlow
      .flatMapLatest { user ->
        // Fetch the latest library based on the value in the User has selected in the database.
        // If the user changes libraries an active subscription to this flow should update the current library
        val request = SingleLibraryRequest(user.id, user.selectedLibraryId)
        singleLibraryStore
          .stream(StoreReadRequest.cached(request, refresh = refresh))
          .debugLogging("SingleLibraryStore")
          .mapNotNull { it.dataOrNull() }
      }
  }

  override fun observeCurrentLibraryItems(refresh: Boolean): Flow<List<LibraryItem>> {
    return userRepository.userFlow
      .flatMapLatest { user ->
        val request = LibraryItemsRequest(user.id, user.selectedLibraryId, user.serverUrl)
        currentLibraryItemsStore
          .stream(StoreReadRequest.cached(request, refresh = refresh))
          .debugLogging("CurrentLibraryItemsStore")
          .mapNotNull { it.dataOrNull() }
      }
  }

  override fun observeAllLibraries(refresh: Boolean): Flow<List<Library>> {
    val userId = userSession.userId ?: return flowOf(emptyList())
    return allLibrariesStore
      .stream(StoreReadRequest.cached(userId, refresh = refresh))
      .debugLogging("AllLibrariesStore")
      .mapNotNull {
        it.dataOrNull()?.map { it.asDomainModel() }
      }
  }

  override fun createLibraryItemPager(
    user: User,
    filter: ContentFilter?,
    sortMode: ContentSortMode,
    sortDirection: SortDirection,
  ): LibraryItemPager {
    val input = LibraryItemPagingInput(filter, sortMode, sortDirection)
    return libraryItemPagingFactory.create(user, input)
  }

  override fun observeFilteredLibraryCount(
    filter: ContentFilter?,
    sortMode: ContentSortMode,
    sortDirection: SortDirection,
  ): Flow<Int?> {
    return userRepository.observeCurrentUser()
      .flatMapLatest { user ->
        val input = LibraryItemPagingInput(filter, sortMode, sortDirection)
        db.libraryItemPageQueries
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

  override suspend fun setCurrentLibrary(library: Library) {
    val userId = userSession.userId ?: return
    withContext(dispatcherProvider.databaseWrite) {
      db.usersQueries.updateSelectedLibrary(library.id, userId)
    }
  }

  override suspend fun getAddPodcastContext(libraryId: LibraryId): Result<AddPodcastContext> {
    return api.getLibrary(libraryId).map { netLibrary ->
      AddPodcastContext(
        folders = netLibrary.folders.map { LibraryFolder(it.id, it.fullPath) },
        searchRegion = netLibrary.settings.podcastSearchRegion,
      )
    }
  }
}

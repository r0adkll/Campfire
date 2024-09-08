package app.campfire.home

import app.campfire.CampfireDatabase
import app.campfire.account.api.CoverImageHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.session.UserSession
import app.campfire.home.api.HomeRepository
import app.campfire.home.api.model.Shelf
import app.campfire.home.mapping.asDomainModel
import app.campfire.network.AudioBookShelfApi
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import me.tatarka.inject.annotations.Inject

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class StoreHomeRepository(
  private val userSession: UserSession,
  private val api: AudioBookShelfApi,
  private val db: CampfireDatabase,
  private val imageHydrator: CoverImageHydrator,
  private val dispatcherProvider: DispatcherProvider,
) : HomeRepository {

  // TODO: Implement a store with api/db, for now just load directly from API
  private val shelfCache = mutableMapOf<String, List<Shelf<*>>>()

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeHomeFeed(): Flow<List<Shelf<*>>> {
    // TODO: This pattern is less than ideal. Should probably find a more graceful way to cast this
    val serverUrl = (userSession as UserSession.LoggedIn).serverUrl
    return db.usersQueries.selectForServer(serverUrl)
      .asFlow()
      .mapToOne(dispatcherProvider.databaseRead)
      .flatMapLatest { user ->
        flow {
          val result = api.getPersonalizedHome(user.selectedLibraryId)
          if (result.isSuccess) {
            val data = result.getOrThrow()
              .map { it.asDomainModel(imageHydrator) }
            shelfCache[serverUrl] = data
            emit(data)
          } else {
            throw result.exceptionOrNull()
              ?: Exception("Unable to fetch home feed")
          }
        }.flowOn(dispatcherProvider.io)
      }
      .onStart {
        // If we have shelf data in the cache, emit it for faster UI experience
        shelfCache[serverUrl]?.let { emit(it) }
      }
  }
}

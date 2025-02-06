package app.campfire.home

import app.campfire.account.api.TokenHydrator
import app.campfire.account.api.UserSessionManager
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.session.UserSession
import app.campfire.core.session.serverUrl
import app.campfire.home.api.HomeRepository
import app.campfire.home.api.model.Shelf
import app.campfire.home.mapping.asDomainModel
import app.campfire.home.progress.MediaProgressDataSource
import app.campfire.network.AudioBookShelfApi
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import me.tatarka.inject.annotations.Inject

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class StoreHomeRepository(
  private val userSessionManager: UserSessionManager,
  private val api: AudioBookShelfApi,
  private val imageHydrator: TokenHydrator,
  private val mediaProgressDataSource: MediaProgressDataSource,
  private val dispatcherProvider: DispatcherProvider,
) : HomeRepository {

  // TODO: Implement a store with api/db, for now just load directly from API
  private val shelfCache = mutableMapOf<String, List<Shelf<*>>>()

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeHomeFeed(): Flow<List<Shelf<*>>> {
    return userSessionManager.observe()
      .filterIsInstance<UserSession.LoggedIn>()
      .flatMapLatest { session ->
        flow {
          val result = api.getPersonalizedHome(session.user.selectedLibraryId)
          if (result.isSuccess) {
            val data = result.getOrThrow()
              .map { it.asDomainModel(imageHydrator, mediaProgressDataSource) }
            shelfCache[session.user.serverUrl] = data
            emit(data)
          } else {
            throw result.exceptionOrNull()
              ?: Exception("Unable to fetch home feed")
          }
        }
          .flowOn(dispatcherProvider.io)
          .onStart {
            // If we have shelf data in the cache, emit it for faster UI experience
            shelfCache[session.user.serverUrl]?.let { emit(it) }
          }
      }
  }
}

package app.campfire.account

import app.campfire.CampfireDatabase
import app.campfire.account.api.ServerRepository
import app.campfire.account.api.UserSessionManager
import app.campfire.account.server.asDomainModel
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.model.Server
import app.campfire.core.model.Tent
import app.campfire.core.session.UserSession
import app.campfire.core.session.serverUrl
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.MemoryPolicy
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.StoreBuilder
import org.mobilenativefoundation.store.store5.StoreReadRequest

@ContributesBinding(UserScope::class)
@Inject
class StoreServerRepository(
  private val db: CampfireDatabase,
  private val userSessionManager: UserSessionManager,
  private val dispatcherProvider: DispatcherProvider,
) : ServerRepository {

  private val serverStore = StoreBuilder
    .from(
      fetcher = Fetcher.of { /* Do nothing */ },
      sourceOfTruth = SourceOfTruth.of(
        reader = { url: String ->
          db.serversQueries.selectByUrl(url)
            .asFlow()
            .mapToOne(dispatcherProvider.databaseRead)
            .map { it.asDomainModel() }
        },
        writer = { _, _ ->
          // Do nothing
        },
      ),
    )
    .cachePolicy(
      MemoryPolicy.builder<String, Server>()
        .setMaxSize(2)
        .setExpireAfterAccess(10.minutes)
        .build(),
    )
    .build()

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeCurrentServer(): Flow<Server> {
    return userSessionManager.observe()
      .flatMapLatest { userSession ->
        if (userSession is UserSession.LoggedIn) {
          serverStore
            .stream(StoreReadRequest.cached(userSession.user.serverUrl, false))
            .mapLatest { it.dataOrNull() }
            .filterNotNull()
        } else {
          emptyFlow()
        }
      }
  }

  override suspend fun changeTent(tent: Tent) {
    withContext(dispatcherProvider.databaseWrite) {
      db.serversQueries.updateTent(
        tent = tent,
        url = userSessionManager.current.serverUrl!!,
      )
    }
  }
}

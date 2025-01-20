package app.campfire.account

import app.campfire.CampfireDatabase
import app.campfire.account.api.ServerRepository
import app.campfire.account.api.UserSessionManager
import app.campfire.account.server.store.ServerStore
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.model.Server
import app.campfire.core.model.Tent
import app.campfire.core.session.UserSession
import app.campfire.core.session.requiredUserId
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.impl.extensions.get

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class StoreServerRepository(
  private val db: CampfireDatabase,
  private val userSessionManager: UserSessionManager,
  private val serverStoreFactory: ServerStore.Factory,
  private val dispatcherProvider: DispatcherProvider,
) : ServerRepository {

  private val serverStore by lazy { serverStoreFactory.create() }

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeCurrentServer(): Flow<Server> {
    return userSessionManager.observe()
      .flatMapLatest { userSession ->
        if (userSession is UserSession.LoggedIn) {
          val key = ServerStore.Operation.User(userSession.user.id)
          serverStore
            .stream(StoreReadRequest.cached(key, false))
            .mapLatest { it.dataOrNull() as? ServerStore.Output.Single }
            .map { it?.server }
            .filterNotNull()
        } else {
          emptyFlow()
        }
      }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeAllServers(): Flow<List<Server>> {
    val key = ServerStore.Operation.All
    return serverStore
      .stream(StoreReadRequest.cached(key, false))
      .mapLatest { it.dataOrNull() as? ServerStore.Output.Collection }
      .map { it?.servers }
      .filterNotNull()
  }

  override suspend fun getCurrentServer(): Server? {
    val user = (userSessionManager.current as? UserSession.LoggedIn)?.user ?: return null
    val key = ServerStore.Operation.User(user.id)
    val output = serverStore.get(key) as? ServerStore.Output.Single
    return output?.server
  }

  override suspend fun getAllServers(): List<Server> {
    val output = serverStore.get(ServerStore.Operation.All) as ServerStore.Output.Collection
    return output.servers
  }

  override suspend fun changeTent(tent: Tent) {
    withContext(dispatcherProvider.databaseWrite) {
      db.serversQueries.updateTent(
        tent = tent,
        userId = userSessionManager.current.requiredUserId,
      )
    }
  }

  override suspend fun changeName(newName: String) {
    withContext(dispatcherProvider.databaseWrite) {
      db.serversQueries.updateName(
        name = newName,
        userId = userSessionManager.current.requiredUserId,
      )
    }
  }

  override suspend fun remove(server: Server) {
    serverStore.clear(ServerStore.Operation.User(server.user.id))
  }
}

package app.campfire.account.server.db

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.model.Server
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface ServerDao {

  fun observeOne(userId: String): Flow<Server>
  fun observeAll(): Flow<List<Server>>
  suspend fun delete(userId: String)
}

@ContributesBinding(AppScope::class)
@Inject
class SqlDelightServerDao(
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
) : ServerDao {

  override fun observeOne(userId: String): Flow<Server> {
    return db.serversQueries.selectByUserId(userId, ::ServerWithUser)
      .asFlow()
      .mapToOne(dispatcherProvider.databaseRead)
      .map { it.asDomainModel() }
  }

  override fun observeAll(): Flow<List<Server>> {
    return db.serversQueries.selectAll(::ServerWithUser)
      .asFlow()
      .mapToList(dispatcherProvider.databaseRead)
      .map { servers -> servers.map { it.asDomainModel() } }
  }

  override suspend fun delete(userId: String) {
    withContext(dispatcherProvider.databaseWrite) {
      db.serversQueries.delete(userId)
    }
  }
}

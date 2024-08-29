package app.campfire.account

import app.campfire.CampfireDatabase
import app.campfire.account.api.ServerRepository
import app.campfire.account.server.ServerCache
import app.campfire.account.server.asDomainModel
import app.campfire.common.settings.CampfireSettings
import app.campfire.core.di.UserScope
import app.campfire.core.model.Server
import app.cash.sqldelight.async.coroutines.awaitAsOne
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import me.tatarka.inject.annotations.Inject

@ContributesBinding(UserScope::class)
@Inject
class StoreServerRepository(
  private val db: CampfireDatabase,
  private val cache: ServerCache,
  private val settings: CampfireSettings,
) : ServerRepository {

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeCurrentServer(): Flow<Server> {
    return settings.observeCurrentServerUrl()
      .filterNotNull()
      .mapLatest { currentServerUrl ->
        val cached = cache.get(currentServerUrl)
        if (cached != null) return@mapLatest cached

        db.serversQueries.selectByUrl(currentServerUrl)
          .awaitAsOne()
          .asDomainModel()
          .also { cache.put(it) }
      }
  }
}

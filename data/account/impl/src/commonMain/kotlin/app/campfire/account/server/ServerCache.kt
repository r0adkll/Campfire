package app.campfire.account.server

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.model.Server
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

interface ServerCache {

  suspend fun get(serverUrl: String): Server?
  suspend fun put(server: Server)
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class InMemoryServerCache : ServerCache {

  private val cache = mutableMapOf<String, Server>()

  override suspend fun get(serverUrl: String): Server? {
    return cache[serverUrl]
  }

  override suspend fun put(server: Server) {
    cache[server.url] = server
  }
}

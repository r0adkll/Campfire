package app.campfire.account.server.store

import app.campfire.account.server.db.ServerDao
import app.campfire.core.logging.Cork
import app.campfire.core.model.Server
import app.campfire.core.model.UserId
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.FetcherResult
import org.mobilenativefoundation.store.store5.MemoryPolicy
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

object ServerStore : Cork {

  override val tag: String = "ServerStore"

  @Inject
  class Factory(
    dao: ServerDao,
  ) {
    private val sourceOfTruthFactory = ServerSourceOfTruthFactory(dao)

    fun create(): Store<Operation, Output> {
      return StoreBuilder
        .from(
          fetcher = Fetcher.ofResult { FetcherResult.Error.Message("Fetching not configured") },
          sourceOfTruth = sourceOfTruthFactory.create(),
        )
        .cachePolicy(
          MemoryPolicy.builder<Operation, Output>()
            .setMaxSize(10)
            .build(),
        )
        .build()
    }
  }

  sealed interface Operation {
    data class User(val userId: UserId) : Operation
    data object All : Operation
  }

  sealed interface Output {
    data class Single(val server: Server) : Output
    data class Collection(val servers: List<Server>) : Output
  }
}

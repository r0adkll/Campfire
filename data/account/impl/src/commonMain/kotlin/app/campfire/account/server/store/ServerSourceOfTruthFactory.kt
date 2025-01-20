package app.campfire.account.server.store

import app.campfire.account.server.db.ServerDao
import app.campfire.account.server.store.ServerStore.Operation
import app.campfire.account.server.store.ServerStore.Output
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.store5.SourceOfTruth

class ServerSourceOfTruthFactory(
  private val dao: ServerDao,
) {

  fun create(): SourceOfTruth<Operation, Output, Output> {
    return SourceOfTruth.of(
      reader = { operation ->
        when (operation) {
          Operation.All -> dao.observeAll().map { Output.Collection(it) }
          is Operation.User -> dao.observeOne(operation.userId).map { Output.Single(it) }
        }
      },
      writer = { _, _ ->
        // Do Nothing
      },
      delete = { operation ->
        require(operation is Operation.User)
        dao.delete(operation.userId)
      },
    )
  }
}

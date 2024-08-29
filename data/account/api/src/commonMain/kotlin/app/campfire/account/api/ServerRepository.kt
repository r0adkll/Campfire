package app.campfire.account.api

import app.campfire.core.model.Server
import kotlinx.coroutines.flow.Flow

interface ServerRepository {

  /**
   * Observe the current server/account that the user as selected
   */
  fun observeCurrentServer(): Flow<Server>
}

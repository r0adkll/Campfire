package app.campfire.account.api

import app.campfire.core.model.Server
import app.campfire.core.model.Tent
import kotlinx.coroutines.flow.Flow

interface ServerRepository {

  /**
   * Observe the current server/account that the user as selected
   */
  fun observeCurrentServer(): Flow<Server>

  /**
   * Fetch the current active server
   */
  suspend fun getCurrentServer(): Server?

  /**
   * Change the [Tent] for the current server
   */
  suspend fun changeTent(tent: Tent)
}

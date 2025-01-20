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
   * Observe a list of all servers logged into the app
   */
  fun observeAllServers(): Flow<List<Server>>

  /**
   * Fetch the current active server
   */
  suspend fun getCurrentServer(): Server?

  /**
   * Fetch all servers logged into the app
   */
  suspend fun getAllServers(): List<Server>

  /**
   * Change the [Tent] for the current server
   */
  suspend fun changeTent(tent: Tent)

  /**
   * Change the name for the current server
   */
  suspend fun changeName(newName: String)

  /**
   * Delete the [server] from the device
   */
  suspend fun remove(server: Server)
}

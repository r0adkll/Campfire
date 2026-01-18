package app.campfire.account.storage

import app.campfire.account.api.AbsToken
import app.campfire.core.model.UserId

/**
 * Interface for storing and fetching tokens from settings
 */
interface TokenStorage {

  suspend fun get(userId: UserId): AbsToken?
  suspend fun put(userId: UserId, token: AbsToken)
  suspend fun remove(userId: UserId)

  /**
   * Get the legacy long-lived token for the current user, if it exists.
   */
  suspend fun getLegacy(userId: UserId): String?
  suspend fun removeLegacy(userId: UserId)
}

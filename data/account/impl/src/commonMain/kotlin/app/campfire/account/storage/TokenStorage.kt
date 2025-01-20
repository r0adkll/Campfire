package app.campfire.account.storage

import app.campfire.core.model.UserId

/**
 * Interface for storing and fetching tokens from settings
 */
interface TokenStorage {

  suspend fun get(userId: UserId): String?
  suspend fun put(userId: UserId, token: String)
  suspend fun remove(userId: UserId)
}

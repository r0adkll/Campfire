package app.campfire.account.storage

/**
 * Interface for storing and fetching tokens from settings
 */
interface TokenStorage {

  suspend fun get(serverUrl: String): String?
  suspend fun put(serverUrl: String, token: String)
}

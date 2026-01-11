package app.campfire.account.storage

import app.campfire.core.model.UserId

interface ExtraHeaderStorage {
  suspend fun get(userId: UserId): Map<String, String>?
  suspend fun put(userId: UserId, headers: Map<String, String>)
  suspend fun remove(userId: UserId)
}

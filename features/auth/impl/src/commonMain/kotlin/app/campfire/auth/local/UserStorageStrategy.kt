package app.campfire.auth.local

import app.campfire.network.models.ServerSettings
import app.campfire.network.models.User as NetworkUser

interface UserStorageStrategy {

  suspend fun store(
    serverName: String,
    serverUrl: String,
    serverSettings: ServerSettings,
    user: NetworkUser,
    userDefaultLibraryId: String,
  )
}

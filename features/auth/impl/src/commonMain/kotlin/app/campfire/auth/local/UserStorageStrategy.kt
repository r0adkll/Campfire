package app.campfire.auth.local

import app.campfire.core.model.Tent
import app.campfire.network.models.ServerSettings
import app.campfire.network.models.User as NetworkUser

interface UserStorageStrategy {

  suspend fun store(
    tent: Tent,
    serverName: String,
    serverUrl: String,
    serverSettings: ServerSettings,
    user: NetworkUser,
    userDefaultLibraryId: String,
  )
}

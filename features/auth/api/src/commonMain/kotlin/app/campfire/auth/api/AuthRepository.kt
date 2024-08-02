package app.campfire.auth.api

import app.campfire.core.model.Tent

interface AuthRepository {

  suspend fun ping(serverUrl: String): Boolean

  suspend fun authenticate(
    serverUrl: String,
    serverName: String,
    username: String,
    password: String,
    tent: Tent,
  ) : Result<Unit>
}

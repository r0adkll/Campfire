package app.campfire.auth.api

import app.campfire.auth.api.model.ServerStatus
import app.campfire.core.model.NetworkSettings
import app.campfire.core.model.Tent

interface AuthRepository {

  suspend fun status(
    serverUrl: String,
    networkSettings: NetworkSettings? = null,
  ): Result<ServerStatus>

  suspend fun authenticate(
    serverUrl: String,
    serverName: String,
    username: String,
    password: String,
    tent: Tent,
    networkSettings: NetworkSettings? = null,
  ): Result<Unit>

  suspend fun authenticate(
    serverUrl: String,
    serverName: String,
    codeVerifier: String,
    code: String,
    state: String,
    tent: Tent,
    networkSettings: NetworkSettings? = null,
  ): Result<Unit>
}

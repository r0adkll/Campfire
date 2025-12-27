package app.campfire.auth.api

import app.campfire.auth.api.model.ServerStatus
import app.campfire.core.model.Tent

interface AuthRepository {

  suspend fun status(serverUrl: String): Result<ServerStatus>

  suspend fun authenticate(
    serverUrl: String,
    serverName: String,
    username: String,
    password: String,
    tent: Tent,
  ): Result<Unit>

  suspend fun authenticate(
    serverUrl: String,
    serverName: String,
    codeVerifier: String,
    code: String,
    state: String,
    tent: Tent,
  ): Result<Unit>
}

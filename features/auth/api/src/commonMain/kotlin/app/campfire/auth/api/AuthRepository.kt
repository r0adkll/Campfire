package app.campfire.auth.api

interface AuthRepository {

  suspend fun healthCheck(serverUrl: String): Boolean

  suspend fun authenticate(
    serverUrl: String,
    userName: String,
    password: String,
  )
}

package app.campfire.network

import app.campfire.network.envelopes.LoginResponse

interface AuthAudioBookShelfApi {

  /**
   * Ping an audiobookshelf server to validate that it exists and is running
   */
  suspend fun ping(serverUrl: String): Boolean

  /**
   * This endpoint logs in a client to the server, returning information about the user and server.
   * @param serverUrl the url of the audiobookshelf server to call
   * @param username the [username] of the user to login as
   * @param password the password for [username] to login with
   */
  suspend fun login(
    serverUrl: String,
    username: String,
    password: String,
  ): Result<LoginResponse>
}

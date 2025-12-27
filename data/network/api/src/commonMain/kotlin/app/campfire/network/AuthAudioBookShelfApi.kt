package app.campfire.network

import app.campfire.network.envelopes.AuthorizationResponse
import app.campfire.network.envelopes.LoginResponse
import app.campfire.network.models.ServerStatus

interface AuthAudioBookShelfApi {

  /**
   * Ping an audiobookshelf server to validate that it exists and is running
   */
  @Deprecated("Use .status() instead", replaceWith = ReplaceWith("status(serverUrl)"))
  suspend fun ping(serverUrl: String): Boolean

  /**
   * Get the status of an audiobookshelf server
   */
  suspend fun status(serverUrl: String): Result<ServerStatus>

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

  /**
   * Call /auth/openid to start oauth2 session and receive the authorization URL
   * to display to the user.
   */
  suspend fun authorization(
    serverUrl: String,
    codeChallenge: String,
    codeVerifier: String,
    state: String,
  ): Result<AuthorizationResponse>

  /**
   * This endpoint finalizes the OAuth2 authentication flow for the client.
   * https://api.audiobookshelf.org/#oauth2-callback
   *
   * @param serverUrl the url of the audiobookshelf server to call
   * @param state the state string you generated in the first request
   * @param code The code you received when redirect_uri was called
   * @param codeVerifier This is the verifier you generated when providing the code_challenge in the first request
   * @param cookie The cookie you received when redirect_uri was called
   */
  suspend fun oauth(
    serverUrl: String,
    state: String,
    code: String,
    codeVerifier: String,
  ): Result<LoginResponse>
}

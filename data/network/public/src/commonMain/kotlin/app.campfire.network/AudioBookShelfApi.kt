package app.campfire.network

import app.campfire.network.envelopes.LoginResponse
import app.campfire.network.models.Library
import app.campfire.network.models.LibraryItemMinified
import app.campfire.network.models.Shelf

interface AudioBookShelfApi {

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

  /**
   * Fetch all the libraries accessible to the user
   * @return a result with a list of library objects
   */
  suspend fun getAllLibraries(): Result<List<Library>>

  /**
   * Fetch a single library by its [libraryId]
   * @return a result with the requested library
   */
  suspend fun getLibrary(libraryId: String): Result<Library>

  /**
   * Fetch a library's items
   * @param libraryId the id of the library to fetch the items for
   * @return as result with the list of library items
   */
  suspend fun getLibraryItems(libraryId: String): Result<List<LibraryItemMinified>>

  /**
   * Get a Library's Personalized View
   * This endpoint returns a library's personalized view for home page display.
   */
  suspend fun getPersonalizedHome(libraryId: String): Result<List<Shelf>>
}
